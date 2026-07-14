[CmdletBinding()]
param(
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Database = 'mes',
    [string]$DbUser = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$MysqlPath = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'
)

$ErrorActionPreference = 'Stop'

$initialPassword = $env:MES_SEED_INITIAL_PASSWORD
if ([string]::IsNullOrWhiteSpace($initialPassword) -or $initialPassword.Length -lt 12) {
    throw 'MES_SEED_INITIAL_PASSWORD must contain at least 12 characters.'
}
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    throw 'Database user is required.'
}
if (-not (Test-Path -LiteralPath $MysqlPath)) {
    throw "mysql client not found: $MysqlPath"
}
if ($Database -notmatch '^[A-Za-z0-9_]+$') {
    throw 'Database name contains unsupported characters.'
}

$usernames = @(
    'pmc001', 'mfg001', 'mfg101', 'mfg102', 'mfg111', 'mfg112', 'mfg113',
    'mfg114', 'mfg115', 'mfg116', 'qms001', 'wms001', 'eam001'
)

function New-Pbkdf2Hash([string]$Password) {
    $salt = [byte[]]::new(16)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($salt)
    }
    finally {
        $rng.Dispose()
    }
    $derive = [System.Security.Cryptography.Rfc2898DeriveBytes]::new(
        $Password,
        $salt,
        210000,
        [System.Security.Cryptography.HashAlgorithmName]::SHA256
    )
    try {
        $hash = $derive.GetBytes(32)
        return ('pbkdf2-sha256$210000${0}${1}' -f
            [Convert]::ToBase64String($salt),
            [Convert]::ToBase64String($hash))
    }
    finally {
        $derive.Dispose()
    }
}

$cases = foreach ($username in $usernames) {
    $hash = New-Pbkdf2Hash $initialPassword
    "WHEN '$username' THEN '$hash'"
}
$usernameList = ($usernames | ForEach-Object { "'$_'" }) -join ','
$caseSql = $cases -join "`n"

$sql = @"
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
START TRANSACTION;
SET @admin_user_id = (
  SELECT user_id FROM sys_user
  WHERE username = 'admin' AND deleted_flag = 0
  ORDER BY user_id LIMIT 1
);
UPDATE sys_user
SET password_hash = CASE username
$caseSql
    ELSE password_hash
  END,
  status = 'ENABLE',
  login_fail_count = 0,
  locked_time = NULL,
  remark = 'RENIX_RBAC_V1: PBKDF2 initialized role account',
  updated_by = @admin_user_id,
  updated_time = '2026-07-14 08:10:00'
WHERE username IN ($usernameList)
  AND password_hash = '!'
  AND deleted_flag = 0;
SET @activated_rows = ROW_COUNT();
COMMIT;
SELECT @activated_rows AS activated_rows,
       (SELECT COUNT(*) FROM sys_user WHERE username IN ($usernameList) AND status='ENABLE' AND password_hash LIKE 'pbkdf2-sha256$%' AND deleted_flag=0) AS active_seed_users,
       (SELECT COUNT(*) FROM sys_user WHERE username IN ($usernameList) AND password_hash='!' AND deleted_flag=0) AS placeholder_users;
"@

$oldMysqlPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $DbPassword
    $output = & $MysqlPath --protocol=TCP -h $HostName -P $Port -u $DbUser -D $Database -N -B -e $sql
    if ($LASTEXITCODE -ne 0) {
        throw "mysql client failed with exit code $LASTEXITCODE"
    }
    $output
}
finally {
    if ($null -eq $oldMysqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
}

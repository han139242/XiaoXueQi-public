param(
    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'

$userApiKey = [Environment]::GetEnvironmentVariable('AI_API_KEY', 'User')
if ([string]::IsNullOrWhiteSpace($userApiKey)) {
    throw 'Windows user-level AI_API_KEY is missing.'
}

# Local IDEs and terminals may inherit a stale process-level value.
$env:AI_API_KEY = $userApiKey

$phoneCryptoActiveKeyId = [Environment]::GetEnvironmentVariable('PHONE_CRYPTO_ACTIVE_KEY_ID', 'User')
if ([string]::IsNullOrWhiteSpace($phoneCryptoActiveKeyId)) {
    throw 'Windows user-level PHONE_CRYPTO_ACTIVE_KEY_ID is missing.'
}

$phoneCryptoKeyVariable = 'PHONE_CRYPTO_KEY_' + $phoneCryptoActiveKeyId.ToUpperInvariant().Replace('-', '_')
$phoneCryptoRequiredVariables = @(
    'PHONE_CRYPTO_KEY_IDS',
    $phoneCryptoKeyVariable,
    'PHONE_LOOKUP_HMAC_KEY'
)

$env:PHONE_CRYPTO_ACTIVE_KEY_ID = $phoneCryptoActiveKeyId
foreach ($variableName in $phoneCryptoRequiredVariables) {
    $value = [Environment]::GetEnvironmentVariable($variableName, 'User')
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Windows user-level $variableName is missing."
    }
    Set-Item -Path "Env:$variableName" -Value $value
}

$legacyReadEnabled = [Environment]::GetEnvironmentVariable('PHONE_CRYPTO_LEGACY_READ_ENABLED', 'User')
$env:PHONE_CRYPTO_LEGACY_READ_ENABLED = if ([string]::IsNullOrWhiteSpace($legacyReadEnabled)) { 'false' } else { $legacyReadEnabled }

if ($ValidateOnly) {
    Write-Output 'Local backend environment validation passed.'
    exit 0
}

$backendDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'workspace\idea\ems'
Push-Location $backendDirectory
try {
    & '.\mvnw.cmd' spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Local backend failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

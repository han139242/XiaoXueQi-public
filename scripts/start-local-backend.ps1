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

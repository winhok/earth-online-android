param([string]$ExpectedOwner = 'winhok', [string]$Repository = 'earth-online-android')
$ErrorActionPreference = 'Stop'
if ($Repository -notmatch '^[A-Za-z0-9_.-]+$') { throw 'Invalid repository name.' }
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
foreach ($Command in @('git', 'gh')) {
    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) { throw "Install $Command first." }
}
function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE. No forced retry was attempted." }
}
Invoke-Checked 'gh' @('auth', 'status', '--hostname', 'github.com')
$Login = (& gh api user --jq .login).Trim()
if ($LASTEXITCODE -ne 0 -or $Login -ne $ExpectedOwner) { throw "Authenticated account is '$Login', expected '$ExpectedOwner'." }
$Full = "$Login/$Repository"
& gh repo view $Full --json name 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { throw "Repository $Full already exists. Refusing to overwrite or push to it." }
if (Test-Path (Join-Path $Root '.git')) { throw 'A local .git already exists. Review it and publish manually; this script will not overwrite it.' }
& "$PSScriptRoot/bootstrap-wrapper.ps1"
Invoke-Checked 'git' @('init', '-b', 'main')
$Id = (& gh api user --jq .id).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve GitHub account ID.' }
Invoke-Checked 'git' @('config', 'user.name', $Login)
Invoke-Checked 'git' @('config', 'user.email', "$Id+$Login@users.noreply.github.com")
Invoke-Checked 'git' @('add', '.')
Invoke-Checked 'git' @('commit', '-m', 'feat: Earth Online Android 1.0 release candidate')
Invoke-Checked 'gh' @('repo', 'create', $Full, '--private', '--source', '.', '--remote', 'origin', '--push',
    '--description', 'Offline-first Earth Online quest manager for Android, built with Kotlin and Jetpack Compose')
Write-Host "Created private repository: https://github.com/$Full"
Write-Host 'Check the Actions tab. A successful push does not imply that Android CI passed.'

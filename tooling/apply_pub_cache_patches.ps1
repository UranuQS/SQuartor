# Apply pub-cache build.gradle patches required to build SQuartor on AGP 9.
# See tooling/apply_pub_cache_patches.sh for context.
#
# Usage (from any directory):
#   pwsh -File tooling\apply_pub_cache_patches.ps1

$ErrorActionPreference = 'Stop'

$repoRoot   = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$patchesDir = Join-Path $repoRoot 'tooling\pub-cache-patches'

$candidates = @()
if ($env:PUB_CACHE) { $candidates += $env:PUB_CACHE }
if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA 'Pub\Cache') }
$candidates += (Join-Path $HOME 'AppData\Local\Pub\Cache')
$candidates += (Join-Path $HOME '.pub-cache')

$cacheRoot = $null
foreach ($c in $candidates) {
    if (Test-Path (Join-Path $c 'hosted\pub.dev')) { $cacheRoot = $c; break }
}

if (-not $cacheRoot) {
    Write-Error "Cannot locate pub-cache. Tried: $($candidates -join ', ')`nSet `$env:PUB_CACHE explicitly and retry."
}

Write-Host "Using pub-cache at: $cacheRoot"

function Apply-One {
    param([string]$PkgSubpath)
    $src  = Join-Path $patchesDir $PkgSubpath
    $dest = Join-Path $cacheRoot ("hosted\pub.dev\" + $PkgSubpath)
    if (-not (Test-Path $src)) { Write-Host "  skip $PkgSubpath (patch missing)"; return }
    if (-not (Test-Path (Split-Path $dest))) {
        Write-Host "  skip $PkgSubpath (package not in cache yet — run flutter pub get first)"
        return
    }
    if ((Test-Path $dest) -and ((Get-FileHash $src).Hash -eq (Get-FileHash $dest).Hash)) {
        Write-Host "  ok   $PkgSubpath (already patched)"
        return
    }
    Copy-Item -Path $src -Destination $dest -Force
    Write-Host "  wrote $PkgSubpath"
}

Apply-One 'file_picker-11.0.2\android\build.gradle'
Apply-One 'flutter_inappwebview_android-1.1.3\android\build.gradle'

Write-Host 'Done.'

param(
    [switch]$SkipCompile,
    [string]$MavenRepo = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $repoRoot 'target'
$packageRoot = Join-Path $targetDir 'packaging'
$inputDir = Join-Path $packageRoot 'input'
$distDir = Join-Path $targetDir 'dist'
$appName = 'FractalExplorer'
$appJar = 'fractal-explorer.jar'
$mainClass = 'com.example.fractal.FractalLauncher'

[xml]$pom = Get-Content (Join-Path $repoRoot 'pom.xml')
$preferredJavafxVersion = $pom.project.properties.'javafx.version'
$preferredLwjglVersion = $pom.project.properties.'lwjgl.version'
$rawAppVersion = [string]$pom.project.version
if ($rawAppVersion -match '^(\d+)(?:\.(\d+))?(?:\.(\d+))?') {
    $major = $Matches[1]
    $minor = if ($Matches[2]) { $Matches[2] } else { '0' }
    $patch = if ($Matches[3]) { $Matches[3] } else { '0' }
    $packagedAppVersion = "$major.$minor.$patch"
} else {
    $packagedAppVersion = '1.0.0'
}

$defaultRepo = Join-Path $env:USERPROFILE '.m2\repository'
$resolvedRepo = if ([string]::IsNullOrWhiteSpace($MavenRepo)) { $defaultRepo } else { $MavenRepo }
if (-not (Test-Path $resolvedRepo)) {
    throw "Maven repository not found: $resolvedRepo"
}
$repoCandidates = @($resolvedRepo)

function Resolve-VersionDir {
    param(
        [string]$RelativeArtifactPath,
        [string]$PreferredVersion
    )

    foreach ($repo in $repoCandidates) {
        $baseDir = Join-Path $repo $RelativeArtifactPath
        if (-not (Test-Path $baseDir)) {
            continue
        }
        if ($PreferredVersion -and (Test-Path (Join-Path $baseDir $PreferredVersion))) {
            return (Join-Path $baseDir $PreferredVersion)
        }
        $dirs = Get-ChildItem -Path $baseDir -Directory | Sort-Object Name -Descending
        if ($dirs) {
            return $dirs[0].FullName
        }
    }

    throw "No versions found for $RelativeArtifactPath in $resolvedRepo"
}

function Copy-IfExists {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path $Source) {
        Copy-Item $Source $Destination -Force
        return $true
    }
    return $false
}

if (-not $SkipCompile) {
    try {
        & mvn -q -DskipTests compile
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven compile failed.'
        }
    } catch {
        if (-not (Test-Path (Join-Path $targetDir 'classes'))) {
            throw
        }
        Write-Warning 'Compile step failed in the current environment. Reusing existing target/classes for packaging.'
    }
}

if (-not (Test-Path (Join-Path $targetDir 'classes'))) {
    throw 'Compiled classes were not found under target/classes.'
}

if (Test-Path $packageRoot) {
    Remove-Item $packageRoot -Recurse -Force
}
if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

Push-Location $repoRoot
try {
    & jar --create --file (Join-Path $inputDir $appJar) -C (Join-Path $targetDir 'classes') .
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to assemble application jar.'
    }
} finally {
    Pop-Location
}

$javafxArtifacts = @('javafx-base', 'javafx-graphics', 'javafx-controls')
foreach ($artifact in $javafxArtifacts) {
    $versionDir = Resolve-VersionDir -RelativeArtifactPath ("org\openjfx\" + $artifact) -PreferredVersion $preferredJavafxVersion
    $version = Split-Path $versionDir -Leaf
    $genericJar = Join-Path $versionDir "$artifact-$version.jar"
    $platformJar = Join-Path $versionDir "$artifact-$version-win.jar"
    if (-not (Copy-IfExists $genericJar $inputDir)) {
        throw "Missing JavaFX jar: $genericJar"
    }
    if (-not (Copy-IfExists $platformJar $inputDir)) {
        throw "Missing JavaFX platform jar: $platformJar"
    }
}

$lwjglMissing = $false
$lwjglArtifacts = @('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl')
foreach ($artifact in $lwjglArtifacts) {
    try {
        $versionDir = Resolve-VersionDir -RelativeArtifactPath ("org\lwjgl\" + $artifact) -PreferredVersion $preferredLwjglVersion
        $version = Split-Path $versionDir -Leaf
        $copied = $false
        $copied = (Copy-IfExists (Join-Path $versionDir "$artifact-$version.jar") $inputDir) -or $copied
        $copied = (Copy-IfExists (Join-Path $versionDir "$artifact-$version-natives-windows.jar") $inputDir) -or $copied
        if (-not $copied) {
            $lwjglMissing = $true
        }
    } catch {
        $lwjglMissing = $true
    }
}

if ($lwjglMissing) {
    Write-Warning 'LWJGL jars were not found in the configured Maven cache. The packaged app will run in CPU fallback mode unless those jars are installed later.'
}

& jpackage --type app-image --dest $distDir --input $inputDir --name $appName --main-jar $appJar --main-class $mainClass --vendor 'OpenAI Codex' --description 'Fractal Explorer' --app-version $packagedAppVersion
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage failed.'
}

Write-Host "Packaged app image: $(Join-Path $distDir $appName)"
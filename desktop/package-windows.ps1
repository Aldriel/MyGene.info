<#
.SYNOPSIS
    Builds the portable Windows distribution of MyGene Explorer.

.DESCRIPTION
    Produces target\dist\MyGeneExplorer-<version>-windows-x64.zip, containing
    "MyGene Explorer.exe" and its own trimmed Java runtime: users unzip it and run the
    executable, no Java installation required.

    Requires JDK 21 (jlink and jpackage) and Maven 3.9. The JDK is taken from JAVA_HOME,
    otherwise from the PATH.

.PARAMETER SkipTests
    Packages without running the unit tests.

.EXAMPLE
    .\package-windows.ps1
#>
param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Invoke-Checked([string]$Tool, [string[]]$Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}

function Find-JdkTool([string]$Name) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\$Name.exe")) {
        return "$env:JAVA_HOME\bin\$Name.exe"
    }
    return (Get-Command $Name -ErrorAction Stop).Source
}

$version = ([xml](Get-Content pom.xml -Raw)).project.version
$name = 'MyGene Explorer'
$work = 'target\jpackage'
$libs = "$work\libs"
$runtime = "$work\runtime"
$image = "$work\image"
$zip = "target\dist\MyGeneExplorer-$version-windows-x64.zip"

Write-Host "Packaging $name $version"
if (Test-Path $work) { Remove-Item $work -Recurse -Force }

# 1. Application jar and its runtime dependencies.
$mvnArgs = @('-q', 'package', 'dependency:copy-dependencies', '-DincludeScope=runtime',
    "-DoutputDirectory=$libs")
if ($SkipTests) { $mvnArgs += '-DskipTests' }
Invoke-Checked 'mvn' $mvnArgs
Copy-Item "target\mygene-explorer-desktop-$version.jar" $libs
# JavaFX ships an empty jar plus a platform jar per module: keep the Windows ones only.
Get-ChildItem $libs -Filter 'javafx-*.jar' |
    Where-Object { $_.Name -notmatch '-win\.jar$' } |
    Remove-Item

# 2. Trimmed Java runtime. JavaFX and the libraries stay on the application module path.
#    jdk.localedata provides French formats, jdk.crypto.ec the TLS ciphers used by the APIs.
$modules = @(
    'java.base', 'java.desktop', 'java.logging', 'java.net.http', 'java.prefs', 'java.sql',
    'java.xml', 'java.naming', 'jdk.unsupported', 'jdk.crypto.ec', 'jdk.localedata',
    'jdk.charsets', 'jdk.accessibility'
) -join ','
Invoke-Checked (Find-JdkTool 'jlink') @(
    '--add-modules', $modules, '--strip-debug', '--no-header-files', '--no-man-pages',
    '--compress=zip-6', '--output', $runtime)

# 3. Executable with its icon.
Invoke-Checked (Find-JdkTool 'jpackage') @(
    '--type', 'app-image',
    '--name', $name,
    '--app-version', $version,
    '--vendor', 'Maxime Ethier',
    '--copyright', "(c) $((Get-Date).Year) Maxime Ethier",
    '--description', 'Human genes and their ClinVar variants',
    '--icon', 'packaging\MyGeneExplorer.ico',
    '--runtime-image', $runtime,
    '--module-path', $libs,
    '--module', 'org.mygeneexplorer/org.mygeneexplorer.MyGeneExplorerApp',
    '--dest', $image)

# 4. Portable archive.
New-Item -ItemType Directory -Force (Split-Path $zip) | Out-Null
if (Test-Path $zip) { Remove-Item $zip }
Compress-Archive -Path "$image\$name" -DestinationPath $zip -CompressionLevel Optimal
$sizeMb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host "Created $zip ($sizeMb MB)"

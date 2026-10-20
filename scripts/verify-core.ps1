param([string]$JdkBin = '', [switch]$ExportReference)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Join-Path $projectRoot 'build/core-verification'
$compiler = if ($JdkBin) { Join-Path $JdkBin 'javac.exe' } else { 'javac' }
$runtime = if ($JdkBin) { Join-Path $JdkBin 'java.exe' } else { 'java' }
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'core/src/main/java'),(Join-Path $projectRoot 'core/src/test/java') -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName
& $compiler --release 21 -d $outputRoot $sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Core compilation failed.' }
foreach ($suite in 'CoreVerification','TerracedVerification','IdentityVerification','ReferenceVerification') {
    & $runtime -Xmx512m -cp $outputRoot "dev.castleforge.core.$suite"
    if ($LASTEXITCODE -ne 0) { throw "Verification failed: $suite" }
}
if ($ExportReference) {
    & $runtime -Xmx512m -cp $outputRoot dev.castleforge.core.ReferenceExport (Join-Path $projectRoot 'build/reference') 20261020
    if ($LASTEXITCODE -ne 0) { throw 'Reference export failed.' }
}

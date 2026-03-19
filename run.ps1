param(
  [Parameter(Mandatory=$false)]
  [string]$file = "examples\\hello.rd"
)

 $ErrorActionPreference = "Stop"

if (!(Test-Path $file)) {
  throw "Roadstone file not found: $file"
}

Write-Host "Roadstone: compiling RoadstoneMain.java..."
javac "RoadstoneMain.java"
if ($LASTEXITCODE -ne 0) {
  Write-Host ""
  Write-Host "Roadstone: compile failed."
  Read-Host "Press Enter to close"
  exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Roadstone: running $file..."
java -cp . RoadstoneMain $file
$rc = $LASTEXITCODE
Write-Host ""
if ($rc -ne 0) {
  Write-Host "Roadstone: finished with exit code $rc."
  Read-Host "Press Enter to close"
  exit $rc
}

Write-Host "Roadstone: done."


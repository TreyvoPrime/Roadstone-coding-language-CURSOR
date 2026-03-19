param(
  [Parameter(Mandatory=$true)]
  [string]$file
)

 $ErrorActionPreference = "Stop"

if (!(Test-Path $file)) {
  throw "Roadstone file not found: $file"
}

javac "RoadstoneMain.java"
java -cp . RoadstoneMain $file


@echo off
setlocal

if "%~1"=="" (
  echo Usage: run.bat ^<file.rd^>
  exit /b 1
)

javac RoadstoneMain.java
java -cp . RoadstoneMain "%~1"

endlocal


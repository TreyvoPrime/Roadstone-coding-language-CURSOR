@echo off
setlocal

set "FILE=%~1"
if "%FILE%"=="" set "FILE=examples\hello.rd"

echo Roadstone: compiling RoadstoneMain.java...
javac RoadstoneMain.java
if errorlevel 1 (
  echo.
  echo Roadstone: compile failed.
  pause
  exit /b 1
)

echo.
echo Roadstone: running "%FILE%"...
java -cp . RoadstoneMain "%FILE%"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
  echo Roadstone: finished with errorlevel %RC%.
  pause
  exit /b %RC%
)

echo Roadstone: done.

endlocal


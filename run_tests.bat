@echo off
setlocal EnableExtensions

set "MAVEN_CMD="

for %%I in (mvn.cmd mvn.bat mvn.exe) do (
  where %%I >nul 2>nul
  if not errorlevel 1 (
    set "MAVEN_CMD=%%I"
    goto :maven_found
  )
)

if not defined MAVEN_CMD (
  for %%P in (
    "%ProgramFiles%\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%ProgramFiles%\Apache\maven\bin\mvn.cmd"
    "%ProgramFiles%\Apache Maven\bin\mvn.cmd"
    "%ProgramData%\chocolatey\bin\mvn.cmd"
  ) do (
    if exist %%~P (
      set "MAVEN_CMD=%%~P"
      goto :maven_found
    )
  )
)

:maven_found
if not defined MAVEN_CMD (
    echo Maven not found!
    exit /b 1
)

echo Using Maven at: "%MAVEN_CMD%"
"%MAVEN_CMD%" test
exit /b %errorlevel%

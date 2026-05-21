@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "MAVEN_CMD="

for %%I in (mvn.cmd mvn.bat mvn.exe) do (
  where %%I >nul 2>nul
  if not errorlevel 1 (
    set "MAVEN_CMD=%%I"
    goto :maven_found
  )
)

if not defined MAVEN_CMD if defined M2_HOME if exist "%M2_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%M2_HOME%\bin\mvn.cmd"
if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not defined MAVEN_CMD (
  for %%P in (
    "%ProgramFiles%\Apache\maven\bin\mvn.cmd"
    "%ProgramFiles%\Apache Maven\bin\mvn.cmd"
    "%ProgramData%\chocolatey\bin\mvn.cmd"
    "%LOCALAPPDATA%\Microsoft\WinGet\Links\mvn.cmd"
    "%USERPROFILE%\scoop\shims\mvn.cmd"
  ) do (
    if exist %%~P (
      set "MAVEN_CMD=%%~P"
      goto :maven_found
    )
  )
)

if not defined MAVEN_CMD (
  echo Apache Maven was not found.
  echo.
  echo Install Maven and then run this launcher again:
  echo   winget install Apache.Maven
  echo.
  echo After install, restart terminal/VS Code so PATH is refreshed.
  pause
  exit /b 1
)

:maven_found
java -version >nul 2>nul
if errorlevel 1 (
  echo Java was not found.
  echo Install Java 17+ and run this launcher again.
  pause
  exit /b 1
)

"%MAVEN_CMD%" -q -DskipTests javafx:run
if errorlevel 1 (
  echo.
  echo Launch failed. Check Java/Maven setup and project dependencies.
  pause
  exit /b 1
)

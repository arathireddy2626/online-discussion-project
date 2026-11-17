@echo off
setlocal enabledelayedexpansion

rem ===================================================================
rem  Starts MySQL and checks that the Online Discussion Tool can reach
rem  the database.
rem
rem  Usage:  start-mysql.bat
rem ===================================================================

echo ==========================================================
echo  Online Discussion Tool - database helper
echo ==========================================================
echo.

rem --- 1. Put the MySQL tools on the path for this window -------------------
rem The MySQL bin folder is in the user path, but a terminal that was already
rem open when it was added will not see it. Adding it here makes the mysql
rem command work straight away.
set "MYSQLBIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"
if exist "%MYSQLBIN%\mysql.exe" (
    if "%PATH%"=="%PATH:~0,0%" (
        echo [1/4] The MySQL tools are not on the path in this window.
    )
    set "PATH=%MYSQLBIN%;%PATH%"
    echo [1/4] MySQL tools ready: %MYSQLBIN%
) else (
    echo [1/4] Could not find the MySQL tools folder.
    echo       Looked in: %MYSQLBIN%
)

rem --- 2. Is the service running? --------------------------------------------
rem The sc query output looks like "STATE : 4 RUNNING", so RUNNING is the
rem fourth word, not the third.
set "STATE="
for /f "tokens=4" %%s in ('sc query MySQL80 ^| findstr /C:"STATE"') do set "STATE=%%s"

if /i "%STATE%"=="RUNNING" (
    echo [2/4] The MySQL80 service is already running.
) else (
    echo [2/4] The MySQL80 service is not running. Trying to start it...
    net start MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo       Could not start it from this window.
        echo.
        echo       This is normal if the terminal is not running as Administrator.
        echo       Use this instead:
        echo.
        echo         1. Open the Start menu and search for "services.msc"
        echo         2. Find "MySQL80", right click it, choose "Start"
        echo.
        echo       Or open PowerShell as Administrator and run:
        echo         Start-Service MySQL80
    ) else (
        echo       The MySQL80 service is now running.
    )
)

rem --- 3. Is the port open? --------------------------------------------------
set "PORTOPEN=no"
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    set "PORTOPEN=no"
) else (
    set "PORTOPEN=yes"
)

if "%PORTOPEN%"=="yes" (
    echo [3/4] Port 3306 is open, MySQL is accepting connections.
) else (
    echo [3/4] Port 3306 is NOT open. MySQL is not accepting connections yet.
    echo       Wait a few seconds and run this file again.
)

rem --- 4. Can the tool sign in? ----------------------------------------------
echo [4/4] Testing the connection with the command line interface...
echo.
pushd "%~dp0"
set "CP=%~dp0build\classes"
for %%j in ("%~dp0lib\*.jar") do set "CP=!CP!;%%~fj"

if not exist "%~dp0build\classes\com\onlinediscussion\cli\DiscussionCli.class" (
    echo       The project is not built yet, building it now...
    call "%~dp0build.bat" >nul 2>&1
)

"%~dp0tools\jdk\jdk-21.0.13+9\bin\java.exe" -cp "%CP%" com.onlinediscussion.cli.DiscussionCli login drKasi Instructor@123
set "RESULT=%errorlevel%"
popd

echo.
if "%RESULT%"=="0" (
    echo ==========================================================
    echo  Everything is ready. You can start the demonstration:
    echo.
    echo    cli.bat --script demo-full.txt
    echo.
    echo  Or open the interactive mode:
    echo.
    echo    cli.bat
    echo ==========================================================
) else (
    echo The connection did not work. Check the settings in db.properties.
)

endlocal

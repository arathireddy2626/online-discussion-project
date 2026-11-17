@echo off
setlocal enabledelayedexpansion

rem Starts the command line interface of the Online Discussion Tool Java backend.
rem Usage: cli.bat [command] [arguments...]
rem        cli.bat --script <file>   runs a file of commands

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%tools\jdk\jdk-21.0.13+9"
set "CLASSES=%ROOT%build\classes"
set "LIBS=%ROOT%lib"

if not exist "%CLASSES%\com\onlinediscussion\cli\DiscussionCli.class" (
    echo The project is not built yet. Running build.bat...
    call "%ROOT%build.bat"
    if errorlevel 1 exit /b 1
)

rem The Java launcher does not expand a wildcard, so the jars are listed one by one.
set "CP=%CLASSES%"
for %%j in ("%LIBS%\*.jar") do set "CP=!CP!;%%~fj"

"%JAVA_HOME%\bin\java.exe" -cp "%CP%" com.onlinediscussion.cli.DiscussionCli %*
endlocal

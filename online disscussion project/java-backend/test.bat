@echo off
setlocal enabledelayedexpansion

rem Runs the JUnit 5 test suite of the Online Discussion Tool Java backend.
rem Usage: test.bat

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%tools\jdk\jdk-21.0.13+9"
set "CLASSES=%ROOT%build\classes"
set "TESTCLASSES=%ROOT%build\test-classes"
set "LIBS=%ROOT%lib"

call "%ROOT%build.bat"
if errorlevel 1 exit /b 1

rem The JUnit launcher does not expand a wildcard, so the jars are listed one by one.
set "CP=%CLASSES%;%TESTCLASSES%"
for %%j in ("%LIBS%\*.jar") do set "CP=!CP!;%%~fj"

echo Running the test suite...
"%JAVA_HOME%\bin\java.exe" -jar "%LIBS%\junit-platform-console-standalone-1.10.2.jar" execute ^
    --class-path "%CP%" ^
    --select-package com.onlinediscussion ^
    --details=summary ^
    --disable-ansi-colors
exit /b %errorlevel%

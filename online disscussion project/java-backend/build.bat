@echo off
setlocal enabledelayedexpansion

rem Builds the Online Discussion Tool Java backend.
rem Usage: build.bat

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%tools\jdk\jdk-21.0.13+9"
set "CLASSES=%ROOT%build\classes"
set "TESTCLASSES=%ROOT%build\test-classes"
set "LIBS=%ROOT%lib"

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Could not find the JDK at %JAVA_HOME%.
    exit /b 1
)

if not exist "%CLASSES%" mkdir "%CLASSES%"
if not exist "%TESTCLASSES%" mkdir "%TESTCLASSES%"

rem The JUnit launcher and the Java launcher do not expand a wildcard, so the jars are
rem listed one by one.
set "CP=%CLASSES%;%TESTCLASSES%"
for %%j in ("%LIBS%\*.jar") do set "CP=!CP!;%%~fj"

rem javac expands a wildcard when the path is relative, so the sources are listed that way.
pushd "%ROOT%"

echo Compiling the main sources...
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -d "%CLASSES%" -cp "%CP%" ^
    src\main\java\com\onlinediscussion\model\*.java ^
    src\main\java\com\onlinediscussion\core\*.java ^
    src\main\java\com\onlinediscussion\db\*.java ^
    src\main\java\com\onlinediscussion\cli\*.java
if errorlevel 1 (popd & exit /b 1)

echo Compiling the test sources...
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -d "%TESTCLASSES%" -cp "%CP%" ^
    src\test\java\com\onlinediscussion\*.java
if errorlevel 1 (popd & exit /b 1)

popd

echo Build finished.
endlocal

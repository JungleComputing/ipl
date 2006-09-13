@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set DIR=%~dp0..

call "%DIR%\configuration.bat"

if "%JAVA_HOME%"=="" (
    set JAVA_HOME=%JAVA_ROOT%
)

call "ant" %1 %2 %3 %4 %5

if "%OS%"=="Windows_NT" @endlocal

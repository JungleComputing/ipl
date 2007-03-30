@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IBIS_HOME%X"=="X" set IBIS_HOME=%~dp0..

if "%JAVA_HOME%"=="" (
    echo The JAVA_HOME environment variable must be set
    goto end
)

call "ant" %1 %2 %3 %4 %5

:end

if "%OS%"=="Windows_NT" @endlocal

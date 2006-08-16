@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set IBIS_ROOT=%~dp0..

if "%JAVA_HOME%"=="" (
    if "%JAVA_ROOT%"=="" (
	echo Either the JAVA_HOME or the JAVA_ROOT environment variable must be set
	goto end
    )
    set JAVA_HOME=%JAVA_ROOT%
)

call "ant" %1 %2 %3 %4 %5

:end

if "%OS%"=="Windows_NT" @endlocal

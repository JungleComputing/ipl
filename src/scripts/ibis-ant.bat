@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set DIR=%~dp0..

call "%DIR%\configuration.bat"

call "%IBIS_ROOT%\3rdparty\apache-ant-1.6.1\bin\ant" %1 %2 %3 %4 %5

if "%OS%"=="Windows_NT" @endlocal

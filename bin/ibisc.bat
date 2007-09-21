
@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IBIS_HOME%X"=="X" set IBIS_HOME=%~dp0..

set JAVACLASSPATH=%CLASSPATH%;
for %%i in ("%IBIS_HOME%\lib\*.jar") do call "%IBIS_HOME%\bin\AddToIbisClassPath.bat" %%i

set IBISC_ARGS=
if ""%1""=="""" goto doneStart
set IBISC_ARGS=%IBISC_ARGS% "%1"
shift
goto setupArgs

:doneStart

java -classpath "%JAVACLASSPATH%" ibis.frontend.Ibisc %IBISC_ARGS%

if "%OS%"=="Windows_NT" @endlocal


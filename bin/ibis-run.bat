@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IBIS_HOME%X"=="X" set IBIS_HOME=%~dp0..


if ""%1""=="""" goto usage

set JAVA_EXEC=java

set JAVACLASSPATH=%CLASSPATH%;build;
for %%i in ("%IBIS_HOME%\lib\*.jar") do call "%IBIS_HOME%\bin\AddToIbisClassPath.bat" %%i

rem From Ant:
rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway)
set IBIS_APP_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
set IBIS_APP_ARGS=%IBIS_APP_ARGS% %1
shift
goto setupArgs
rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneStart

if not "%JAVA_HOME%"=="" (
    set JAVA_EXEC=%JAVA_HOME%\bin\%JAVA_EXEC%
)


"%JAVA_EXEC%" -classpath "%JAVACLASSPATH%" %IBIS_APP_ARGS%

if "%OS%"=="Windows_NT" @endlocal

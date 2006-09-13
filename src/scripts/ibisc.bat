
@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set IBIS_HOME=%~dp0..

set JAVACLASSPATH=%CLASSPATH%;%IBIS_HOME%\lib\ibis.jar;%IBIS_HOME%\3rdparty\junit.jar;%IBIS_HOME%\3rdparty\log4j-1.2.9.jar;%IBIS_HOME%\3rdparty\colobus.jar;

rem From Ant:
rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway)
set IBISC_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
set IBISC_ARGS=%IBISC_ARGS% %1
shift
goto setupArgs
rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneStart

set JAVA=java
if not "%JAVA_HOME%"=="" (
    set JAVA=%JAVA_HOME%\bin\java
)

"%JAVA%" -classpath "%JAVACLASSPATH%" ibis.frontend.ibis.Ibisc %IBISC_ARGS%

if "%OS%"=="Windows_NT" @endlocal


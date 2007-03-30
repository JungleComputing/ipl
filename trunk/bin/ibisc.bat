
@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IBIS_HOME%X"=="X" set IBIS_HOME=%~dp0..

set JAVACLASSPATH=%CLASSPATH%;
for %%i in ("%IBIS_HOME%\lib\*.jar") do call "%IBIS_HOME%\bin\AddToIbisClassPath.bat" %%i

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


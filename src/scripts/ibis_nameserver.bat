@echo off

set JAVACLASSPATH=%CLASSPATH%;%IBIS_ROOT%\build;

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

"%JAVA_ROOT%\bin\java" -classpath %JAVACLASSPATH% ibis.impl.nameServer.tcp.NameServer %IBIS_APP_ARGS%

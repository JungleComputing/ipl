@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IPL_HOME%X"=="X" set IPL_HOME=%~dp0..

set JAVACLASSPATH=%CLASSPATH%;
for %%i in ("C:\Development\Java\Projects\IPL-Reza\lib\*.jar") do call "C:\Development\Java\Projects\IPL-Reza\EXE\AddToClassPath.bat" %%i

set SERVER_ARGS=--events --errors

:setupArgs

if ""%1""=="""" goto doneArgs

set SERVER_ARGS=%SERVER_ARGS% "%1"

shift
goto setupArgs

rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneArgs

echo %SERVER_ARGS%
java -classpath "%JAVACLASSPATH%" -Dlog4j.configuration=file:"C:\Development\Java\Projects\IPL-Reza\log4j.properties" ibis.ipl.server.Server %SERVER_ARGS%

if "%OS%"=="Windows_NT" @endlocal

@echo off

if NOT DEFINED JAVA_HOME goto javahome
if "%JAVA_HOME%" == "" goto javahome

set DIR=%~dp0

%DIR%3rdparty\apache-ant-1.6.1\bin\ant %1 %2 %3 %4 %5
goto end

:javahome
echo The enviroment variable JAVA_HOME must be set to the current jdk 
echo distribution installed on your computer, and must be a 1.4 version
echo or newer.
goto end

:end

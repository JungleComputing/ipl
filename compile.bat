@echo off

if NOT DEFINED JAVA_HOME goto javahome
if "%JAVA_HOME%" == "" goto javahome

if defined ANT_HOME (
        echo You have the ANT_HOME environment variable set.
	echo I am resetting it to 3rdparty\apache-ant-1.6.1
)

set ANT_HOME=3rdparty\apache-ant-1.6.1

3rdparty\apache-ant-1.6.1\bin\ant %1 %2 %3 %4 %5
goto end

:javahome
echo The enviroment variable JAVA_HOME must be set to the current jdk 
echo distribution installed on your computer, and must be a 1.4 version
echo or newer.
goto end

:end

@echo off

if defined ANT_HOME (
        echo You have the ANT_HOME environment variable set.
	echo I am resetting it to %IBIS_ROOT%\3rdparty\apache-ant-1.6.1

)

set ANT_HOME=%IBIS_ROOT%\3rdparty\apache-ant-1.6.1

%IBIS_ROOT%\3rdparty\apache-ant-1.6.1\bin\ant %1 %2 %3 %4 %5

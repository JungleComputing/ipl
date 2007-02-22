@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

if "%IBIS_HOME%X"=="X" set IBIS_HOME=%~dp0..

set JAVACLASSPATH=%CLASSPATH%;
for %%i in ("%IBIS_HOME%\lib\*.jar") do call "%IBIS_HOME%\bin\AddToIbisClassPath.bat" %%i

set ConnectHub="-Dibis.connect.control_links=RoutedMessages -Dibis.connect.data_links=PlainTCP"

set NS_ARGS=
set JAVA_ARGS=
set Dport=
set Dpoolport=

:setupArgs

if ""%1""=="""" goto doneArgs

if "%1"=="-single" (
    set NS_ARGS=%NS_ARGS% -single
    goto nextarg
)

if "%1"=="-poolserver" (
    set NS_ARGS=%NS_ARGS% -poolserver
    goto nextarg
)

if "%1"=="-no-poolserver" (
    set NS_ARGS=%NS_ARGS% -no-poolserver
    goto nextarg
)

if "%1"=="-v" (
    set NS_ARGS=%NS_ARGS% -verbose
    goto nextarg
)

if "%1"=="-verbose" (
    set NS_ARGS=%NS_ARGS% -verbose
    goto nextarg
)

if "%1"=="-?" (
    goto usage
)

if "%1"=="-h" (
    goto usage
)

if "%1"=="-help" (
    goto usage
)

if "%1"=="--help" (
    goto usage
)

if "%1"=="-port" (
    set Dport="-Dibis.registry.port=%2"
    shift
    goto nextarg
)

if "%1"=="-ns-port" (
    set Dport="-Dibis.registry.port=%2"
    shift
    goto nextarg
)

if "%1"=="-poolport" (
    set Dpoolport="-Dibis.pool.server.port=%2"
    shift
    goto nextarg
)

if "%1"=="-pool-port" (
    set Dpoolport="-Dibis.pool.server.port=%2"
    shift
    goto nextarg
)

set JAVA_ARGS=%JAVA_ARGS% "%1"

:nextarg
    shift
    goto setupArgs

rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneArgs

set JAVA=java
if not "%JAVA_HOME%"=="" (
    set JAVA=%JAVA_HOME\bin\java
)

"%JAVA%" -classpath "%JAVACLASSPATH%" %Dport% %Dpoolport% %JAVA_ARGS% ibis.impl.registry.tcp.NameServer %NS_ARGS%

goto end

:usage

    echo Usage:
    echo   ibis-nameserver ^<nameserver parameters^>
    echo 
    echo The nameserver parameters are:
    echo -single
    echo     make the nameserver only serve a single Ibis run. (In fact, it will
    echo     server more than one run, but will exit as soon as no more runs are
    echo     active).
    echo -port ^<portno^>
    echo     make the nameserver listen to port number ^<portno^>. The default
    echo     port number is 9826.
    echo -ns-port ^<portno^>
    echo     same as -port.
    echo -poolserver
    echo     make the nameserver start a pool server (see ibis.util.PoolInfo in the
    echo     Ibis API). This is the default.
    echo -no-poolserver
    echo     with this option, the nameserver does not start a pool server.
    echo -poolport ^<poolportno^>
    echo     make the poolserver listen to port number ^<poolportno^>. The default
    echo     port number is the nameserver port number + 1.
    echo -?
    echo     print this message
    echo -h
    echo     print this message
    echo 
    echo Unrecognized options are passed on to java.
:end

if "%OS%"=="Windows_NT" @endlocal

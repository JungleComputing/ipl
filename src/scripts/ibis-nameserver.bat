@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set DIR=%~dp0..

%DIR%\configuration.bat

set JAVACLASSPATH=%CLASSPATH%;%IBIS_ROOT%\build;

set ConnectHub="-Dibis.connect.enable -Dibis.connect.control_links=RoutedMessages -Dibis.connect.data_links=PlainTCP"

set NS_ARGS=
set JAVA_ARGS=
set Ddebug=
set Dhub=
set Dport=
set Dpoolport=
set Dhubport=
set Dhubhost=

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

if "%1"=="-no-controlhub" (
    set NS_ARGS=%NS_ARGS% -no-controlhub
    goto nextarg
)

if "%1"=="-debug" (
    set Ddebug="-Dibis.connect.debug=true -Dibis.connect.verbose=true"
    goto nextarg
)

if "%1"=="-d" (
    set Ddebug=%Ddebug% "-Dibis.name_server.debug=true"
    goto nextarg
)

if "%1"=="-v" (
    set Ddebug=%Ddebug% "-Dibis.name_server.verbose=true"
    goto nextarg
)

if "%1"=="-?" (
    goto usage
)

if "%1"=="-h" (
    goto usage
)

if "%1"=="-controlhub" (
    set Dhub=%ConnectHub%
    set NS_ARGS=%NS_ARGS% -controlhub
    goto nextarg
)

if "%1"=="-port" (
    set Dport="-Dibis.name_server.port=%2"
    shift
    goto nextarg
)

if "%1"=="-ns-port" (
    set Dport="-Dibis.name_server.port=%2"
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

if "%1"=="-hubport" (
    set Dhubport="-Dibis.connect.hub_port=%2"
    set Dhub=%ConnectHub%
    shift
    goto nextarg
)

if "%1"=="-hub-port" (
    set Dhubport="-Dibis.connect.hub_port=%2"
    set Dhub=%ConnectHub%
    shift
    goto nextarg
)

if "%1"=="-hubhost" (
    set Dhubhost="-Dibis.connect.hub_host=%2"
    set Dhub=%ConnectHub%
    shift
    goto nextarg
)

if "%1"=="-hub-host" (
    set Dhubhost="-Dibis.connect.hub_host=%2"
    set Dhub=%ConnectHub%
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

"%JAVA_ROOT%\bin\java" -classpath "%JAVACLASSPATH%" %Ddebug% %Dhub% %Dport% %Dpoolport% %Dhubport% %Dhubhost% %JAVA_ARGS% ibis.impl.nameServer.tcp.NameServer %NS_ARGS%

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
    echo -controlhub
    echo     make the nameserver start a controlhub
    echo     (see ibis/docs/notes/Crossing-firewalls.txt). Note that this
    echo     option also makes the nameserver use communication that goes through
    echo     this controlhub.
    echo -no-controlhub
    echo     don't make the nameserver start a controlhub. This is the default.
    echo -hubport ^<hubportno^>
    echo     make the controlhub listen to port number ^<hubportno^>. The default port
    echo     number is the nameserver port number + 2. If the -controlhub flag is not
    echo     given, it is assumed that one is already listening on port ^<hubportno^>.
    echo     Anyway, the nameserver will use communication that goes through a
    echo     controlhub.
    echo -hubhost ^<hubhostname^>
    echo     specifies the hostname with the controlhub that is to be used by the
    echo     nameserver. All communication with the nameserver will go through this
    echo     controlhub.
    echo -v
    echo     make the nameserver more verbose.
    echo -d
    echo     enable debugging prints in the nameserver.
    echo -hubdebug
    echo     enable debugging prints of communication with the controlhub (extremely
    echo     verbose).
    echo -?
    echo     print this message
    echo -h
    echo     print this message
    echo 
    echo Unrecognized options are passed on to java.
:end

if "%OS%"=="Windows_NT" @endlocal

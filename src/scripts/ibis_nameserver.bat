@echo off

if "%OS%"=="Windows_NT" @setlocal

set JAVACLASSPATH=%CLASSPATH%;%IBIS_ROOT%\build;

set ConnectHub="-Dibis.connect.enable -Dibis.connect.control_links=RoutedMessages -Dibis.connect.data_links=PlainTCP"

set NS_ARGS=
set Ddebug=
set Dhub=
set Dport=
set Dpoolport=
set Dhubport=

:setupArgs

if ""%1""=="""" goto doneArgs

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

if "%1"=="-poolport" (
    set Dpoolport="-Dibis.pool.server.port=%2"
    shift
    goto nextarg
)

if "%1"=="-hubport" (
    set Dhubport="-Dibis.connect.port=%2"
    set Dhub=%ConnectHub%
    shift
    goto nextarg
)

set NS_ARGS=%NS_ARGS% "%1"

:nextarg
    shift
    goto setupArgs

rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneArgs

"%JAVA_ROOT%\bin\java" -classpath "%JAVACLASSPATH%" %Ddebug% %Dhub% %Dport% %Dpoolport% %Dhubport% ibis.impl.nameServer.tcp.NameServer %NS_ARGS%

if "%OS%"=="Windows_NT" @endlocal

@echo off

rem Start of script template. If there are environment assignments before this
rem line, this is NOT the template, and is a generated script. Look in
rem %IBIS_ROOT%\src\scripts\run-ibis.bat instead.
rem


if "%OS%"=="Windows_NT" @setlocal

set PRUN_CPU_RANK=0
set NHOSTS=1

if ""%1""=="""" goto usage

set JAVACLASSPATH=%CLASSPATH%;build;%IBIS_ROOT%\classlibs;%IBIS_ROOT%\build;%IBIS_ROOT%\3rdparty\junit.jar;.;


set LFC_INTR_FIRST=100
set IBP_SEND_SYNC=10000 
set PAN_COMM_NO_IDLE_POLL=1

rem
rem java.library.path and ibis.library.path ...
rem

set Dlibpath="-Djava.library.path=%IBIS_ROOT%\build\lib;.;"

rem This is the location where all ibis native libs must go.
rem It must be *one* dir.
set Dibislibs="-Dibis.library.path=%IBIS_ROOT%\build\lib"

rem
rem Some defaults ...
rem

set attach=0
set noJIT=0
set no_pool=0
set portno_specified=0
set JIT_OPTS=

set JAVA_EXEC=java
set Xbootclasspath="-Xbootclasspath/p:%JAVACLASSPATH%"

set action=

rem
rem nameserver defaults
rem

call "%IBIS_ROOT%\bin\ns-env"

set Dns_server="-Dibis.name_server.host=%IBIS_NAMESERVER_HOST%"
set Dns_port="-Dibis.name_server.port=%IBIS_NAMESERVER_PORT%"
set Dns_retry="-Dibis.name_server.retry=%IBIS_NAMESERVER_RETRY%"
set Dns_pool="-Dibis.name_server.key=no_key_supplied"

rem
rem parse arguments
rem

:arguments

if "%1"=="-attach" (
    set attach=1
    goto nextarg
)
if "%1"=="-nhosts" (
    shift
    set Dpool_total="-Dibis.pool.total_hosts=%1"
    set NHOSTS=%1
    goto nextarg
)
if "%1"=="-nhosts" (
    shift
    set Dpool_host_num="-Dibis.pool.host_number=%1"
    set PRUN_CPU_RANK=%1
    goto nextarg
)
if "%1"=="-jdb" (
    set JAVA_EXEC=jdb
    goto nextarg
)
if "%1"=="-no-jit" (
    set noJIT=1
    goto nextarg
)
if "%1"=="-no-jit" (
    set noJIT=1
    goto nextarg
)
if "%1"=="-no-pool" (
    set no_pool=1
    goto nextarg
)
if "%1"=="-n" (
    set action=echo
    goto nextarg
)
if "%1"=="-ns" (
    shift
    set Dns_server="-Dibis.name_server.host=%1"
    goto nextarg
)
if "%1"=="-ns-port" (
    shift
    set Dns_port="-Dibis.name_server.port=%1"
    goto nextarg
)
if "%1"=="-ns-retry" (
    set Dns_retry="-Dibis.name_server.retry=yes"
    goto nextarg
)
if "%1"=="no--ns-retry" (
    set Dns_retry="-Dibis.name_server.retry=no"
    goto nextarg
)
if "%1"=="-pg" (
    shift
    set gprof="%1"
    goto nextarg
)
if "%1"=="-p" (
    shift
    set prof="%1"
    goto nextarg
)
if "%1"=="-key" (
    shift
    set Dns_pool="-Dibis.name_server.key=%1"
    goto nextarg
)
if "%1"=="-?" (
    goto usage
)
if "%1"=="--" (
    shift
    goto cont
)

goto cont

:nextarg
    shift
    goto arguments

:cont

rem
rem if no_pool is set, kill all pool info
rem

if "%no_pool%"=="1" (
    set Dpool_total=
    set Dpool_host_num=
    set Dpool_host_names=
)

if "%noJIT%"=="1" (
    set JIT_OPTS=%JIT_OPTS% -Djava.compiler=NONE
)
if "%attach%"=="1" (
    set JIT_OPTS=%JIT_OPTS% -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -Djava.compiler=NONE
)
if not "%prof%"=="" (
    set JIT_OPTS=%JIT_OPTS% -Xrunhprof:cpu=samples,depth=8,thread=y,file=%prof%.%PRUN_CPU_RANK% -Djava.compiler=NONE
)
if not "%gprof%"=="" (
    set JIT_OPTS=%JIT_OPTS% -Xrunhprof:cpu=times,depth=8,thread=y,file=%gprof%.%PRUN_CPU_RANK%
)

rem need at least a class to run
if ""%1""=="""" (
    goto usage
)

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

%action% "%JAVA_ROOT%\bin\%JAVA_EXEC%" %JIT_OPTS% %Dlibpath% %Dibislibs% %Dpool_host_num% %Dpool_total% %Dpool_host_names% %Dns_pool% %Dns_port% %Dns_server% %Dns_retry% %Xbootclasspath% -classpath "%JAVACLASSPATH%" %IBIS_APP_ARGS%

goto end

:usage
    echo Usage:
    echo     run-ibis ^<run-ibis params^> ^<jvm params^> ^<classname^> ^<application params^>
    echo The first parameter that is not recognized as an option to run-ibis
    echo terminates the run-ibis parameters.
    echo The run-ibis options are:
    echo -attach
    echo     set jvm parameters so that jdb can attach to the running process
    echo -nhosts ^<nhosts^>
    echo     specifies the total number of hosts involved in this run
    echo -hostno ^<hostno^>
    echo     specifies the rank number of this host (0 .. ^<nhosts^>-1)
    echo -jdb
    echo     execute jdb instead of java
    echo -no-jit
    echo     disable just-in-time compiling
    echo -no-pool
    echo     don't pass on any node-pool information to the application
    echo -n
    echo     only print the run command, don't actually execute it
    echo -ns ^<nameserver^>
    echo     specifies the hostname on which the nameserver runs
    echo -ns-port
    echo     specifies the port number on which the nameserver is listening
    echo -ns-retry
    echo     specifies that the application should retry to connect to the nameserver
    echo     until it succeeds. The default behavior is to exit when connecting to the
    echo     nameserver fails
    echo -pg ^<prefix^>
    echo     pass profiling flags on to java, use ^<prefix^> for the result file
    echo -p ^<prefix^>
    echo     pass different profiling flags on to java, use ^<prefix^> for the result file
    echo -key ^<key^>
    echo     use the specified key to identify this run with the nameserver
    echo -?
    echo     print this message
    echo --
    echo     terminates the parameters for run-ibis; following parameters are passed
    echo     on to java, even if they would be acceptable to run-ibis

:end

if "%OS%"=="Windows_NT" @endlocal

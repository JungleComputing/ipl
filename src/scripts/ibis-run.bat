@echo off

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT

set DIR=%~dp0..

call "%DIR%\configuration"

set PRUN_CPU_RANK=0
set NHOSTS=1

if ""%1""=="""" goto usage

set JAVACLASSPATH=%CLASSPATH%;build;%IBIS_ROOT%\classlibs;%IBIS_ROOT%\build;%IBIS_ROOT%\3rdparty\junit.jar;.;


set LFC_INTR_FIRST=100
set IBP_SEND_SYNC=100 
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
    set Dpool_total="-Dibis.pool.total_hosts=%2"
    set NHOSTS=%2
    shift
    goto nextarg
)
if "%1"=="-hostno" (
    set Dpool_host_num="-Dibis.pool.host_number=%2"
    set PRUN_CPU_RANK=%2
    shift
    goto nextarg
)
if "%1"=="-hosts" (
    set HOSTS="%2"
    shift
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
if "%1"=="-n" (
    set action=echo
    goto nextarg
)
if "%1"=="-ns" (
    set Dns_server="-Dibis.name_server.host=%2"
    shift
    goto nextarg
)
if "%1"=="-ns-port" (
    set Dns_port="-Dibis.name_server.port=%2"
    shift
    goto nextarg
)
if "%1"=="-pg" (
    set gprof="%2"
    shift
    goto nextarg
)
if "%1"=="-p" (
    set prof="%2"
    shift
    goto nextarg
)
if "%1"=="-key" (
    set Dns_pool="-Dibis.name_server.key=%2"
    shift
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
if "%1"=="--" (
    shift
    goto cont
)

goto cont

:nextarg

    shift
    goto arguments

:cont

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

if not "%HOSTS%"=="" (
    %action% "%JAVA_ROOT%\bin\%JAVA_EXEC%" %JIT_OPTS% %Dlibpath% %Dibislibs% %Dpool_host_num% %Dpool_total% -Dpool_host_names="%HOSTS%" %Dns_pool% %Dns_port% %Dns_server% %Xbootclasspath% -classpath "%JAVACLASSPATH%" %IBIS_APP_ARGS%
) else (
    %action% "%JAVA_ROOT%\bin\%JAVA_EXEC%" %JIT_OPTS% %Dlibpath% %Dibislibs% %Dpool_host_num% %Dpool_total% %Dns_pool% %Dns_port% %Dns_server% %Xbootclasspath% -classpath "%JAVACLASSPATH%" %IBIS_APP_ARGS%
)

goto end

:usage
    echo Usage:
    echo     ibis-run ^<ibis-run params^> ^<jvm params^> ^<classname^> ^<application params^>
    echo The first parameter that is not recognized as an option to ibis-run
    echo terminates the ibis-run parameters.
    echo The ibis-run options are:
    echo -attach
    echo     set jvm parameters so that jdb can attach to the running process
    echo -nhosts ^<nhosts^>
    echo     specifies the total number of hosts involved in this run
    echo -hostno ^<hostno^>
    echo     specifies the rank number of this host (0 .. ^<nhosts^>-1)
    echo -hosts ^<list of hostnames^>
    echo     specifies the host names for this run.
    echo -jdb
    echo     execute jdb instead of java
    echo -no-jit
    echo     disable just-in-time compiling
    echo -n
    echo     only print the run command, don't actually execute it
    echo -ns ^<nameserver^>
    echo     specifies the hostname on which the nameserver runs
    echo -ns-port
    echo     specifies the port number on which the nameserver is listening
    echo -pg ^<prefix^>
    echo     pass profiling flags on to java, use ^<prefix^> for the result file
    echo -p ^<prefix^>
    echo     pass different profiling flags on to java, use ^<prefix^> for the result file
    echo -key ^<key^>
    echo     use the specified key to identify this run with the nameserver
    echo -?
    echo -h
    echo -help
    echo --help
    echo     print this message
    echo --
    echo     terminates the parameters for ibis-run; following parameters are passed
    echo     on to java, even if they would be acceptable to ibis-run

:end

if "%OS%"=="Windows_NT" @endlocal

set JAVA_ROOT=F:\java\sun\j2sdk1.4.2
set JAVAC=F:\java\sun\j2sdk1.4.2\bin\javac
set JIT_OPTS=""
set IBIS_ROOT=F:\werk\ibis
set MANTA_ROOT=\usr\local\VU\manta
set PANDA_ROOT=F:\werk\panda\panda4.0
set LFC_ROOT=F:\werk\lfc-gm
set GM_ROOT=F:\werk\GM
set DAS_LIB=F:\werk\daslib\lib\i386_Linux\libdas.a
set PANDA_NETWORK=lfc
set BCEL=F:\werk\ibis\3rdparty\bcel-5.1.jar

rem  start of script template

@echo off

if "%OS%=="Windows_NT" @beginlocal

set JAVACLASSPATH=%CLASSPATH%;%IBIS_ROOT%\classlibs;%IBIS_ROOT%\build;build;.;

rem disable panda interrupts
rem export IBP_NO_INTR=1

rem  fix panda\lfc flow control
rem export LFC_SEND_COPY_ASIDE=1
rem export PAN_SYS_CREDITS=65535

set LFC_INTR_FIRST=100
set IBP_SEND_SYNC=10000 
set PAN_COMM_NO_IDLE_POLL=1

rem not needed any more, and not portable --Rob
rem Dlibpath="-Dsun.boot.library.path=%IBIS_ROOT%\build\lib;%JAVA_ROOT%\jre\bin;%JAVA_ROOT%\jre\lib\i386"

set Dlibpath="-Djava.library.path=%IBIS_ROOT%\build\lib;.;"

rem  This is the location where all ibis native libs must go.
rem  It must be *one* dir.
set Dibislibs="-Dibis.library.path=%IBIS_ROOT%\build\lib"

set Dpool_host_num="-Dibis.pool.host_number=%1"

rem PROFILING=-Xhprof;cpu=samples,file=profile.%1,depth=3
set PROFILING=
shift

set Dprops="-Dibis.property.file=%IBIS_ROOT%\properties"

set Dpool_total="-Dibis.pool.total_hosts=%1"
shift

if not defined PRUN_ENV  (
    set Dns_pool="-Dibis.name_server.key=%1"
    shift
) else (
    set Dns_pool="-Dibis.name_server.key=%PRUN_ENV%"
)

set Dns_port="-Dibis.name_server.port=%1"
shift

set Dns_server="-Dibis.name_server.host=%1"
shift

rem need at least a class to run
if ""%1""=="" (
	echo "Usage:"
	echo "  run_ibis <PROC_NR> <TOTAL_NR_PROCS> <POOL_ID> <NAMESERVER_PORT> <NAMESERVER_HOSTNAME> <CLASS NAME> <APPLICATION PARAMS>"
	echo "or, when using prun:"
	echo "  prun -1 -v run_ibis <TOTAL_NR_PROCS> <NAMESERVER_PORT> <NAMESERVER_HOSTNAME> <CLASS NAME> <APPLICATION PARAMS>"
	exit 1
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

rem  the -Xmx800M must be here for the SUN JIT, it does not allocate enough mem for some satin apps --Rob
rem  choose 800, so jobs don't start paging.

%JAVA_ROOT%\bin\java -Xmx400M %Dlibpath% %Dibislibs% %Dpool_host_num% %Dprops%  %Dpool_total% %Dns_pool% %Dns_port% %Dns_server% -Dibis.pool.host_names="%HOSTS%" -Xbootclasspath/p:%JAVACLASSPATH% %PROFILING% -classpath %JAVACLASSPATH% %IBIS_APP_ARGS%

if "%OS%=="Windows_NT" @endlocal

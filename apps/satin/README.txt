Running Satin applications:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ant clean compile

compiles a sequential version that can be executed with:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ~/projects/ibis/bin/run_ibis 0 1 foo 9826 fs0.das2.cs.vu.nl Tsp 10


(betekenis:

run_ibis PROC_NR AANTAL_PROCS POOL_ID (=random string) NAMESERVER_PORT NAMESERVER_HOSTNAME CLASSFILE <PARAMS> <SATIN_PARAMS>

or simpler, run with:

java -cp YOUR_IBIS_BUILD_DIR:build CLASS_NAME APP_OPTIONS


parallel compileren:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ant clean build


To execute it, you need to start an ibis name server with:

ibis_nameserver [-port NAMESERVER_PORT]

where NAMESERVER_PORT is an arbitrary port number (it is wise to choose
something over 1000).

After the name server has been started you can run it on just your
local processor with:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ~/projects/ibis/bin/run_ibis 0 1 foo 9826 fs0.das2.cs.vu.nl Tsp 10


To run it in parallel on the DAS2 you need to start the ibis_nameserver
on the DAS2 fs0 processor (see above), and you can submit a job with:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ prun -1 -v ~/projects/ibis/bin/run_ibis 2 9826 fs0.das2.cs.vu.nl Tsp 10 -satin-closed

Where `2' is the number of processors for this particular job, and 9826
is the number of the nameserver port for this example. It is usually
wise to specify -satin-closed to tell the system nobody will join the
computation later on.

It may also be necessary to specify -Dsatin.tuplespace.ordened=true

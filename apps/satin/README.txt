Running Satin applications:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ make java

is sequentiele versie compileren

draaien met:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ~/projects/ibis/bin/run_ibis 0 1 foo 9826 fs0.das2.cs.vu.nl Tsp 10


(betekenis:

run_ibis PROC_NR AANTAL_PROCS POOL_ID (=random string) NAMESERVER_PORT NAMESERVER_HOSTNAME CLASSFILE <PARAMS> <SATIN_PARAMS>

or simpler, run with:

java -cp YOUR_IBIS_BUILD_DIR:. CLASS_NAME APP_OPTIONS


parallel compileren:

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ make satin


draaien:

ibis_nameserver [-port NAMESERVER_PORT]

en

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ ~/projects/ibis/bin/run_ibis 0 1 foo 9826 fs0.das2.cs.vu.nl Tsp 10


parallel draaien op DAS-2:

ibis_nameserver [-port NAMESERVER_PORT]

en

[rob@fs0:~/projects/ibis/apps/satin/tsp]$ prun -1 -v ~/projects/ibis/bin/run_ibis 2 9826 fs0.das2.cs.vu.nl Tsp 10 -satin-closed





Good luck!

-- 
Rob

import os, time

ibisdir = "~/ibis"

run_ibis = ibisdir + "/bin/run_ibis"

logdir = "logs"

#problem = "examples/qg/qg6-12.cnf.gz"
problem = "examples/ais/ais10.cnf.gz"

solver = "DPLLSolver"

ProcNos = [ 1, 2, 4, 8, 16, 32 ]

nameserverport = 2001

def get_time_stamp():
    return time.strftime( "%Y-%m-%d %H:%M:%S", time.localtime())

logfile = logdir + "/log-" + get_time_stamp()
lf = open( logfile, "w" )

def build_run_command( pno, solver, problem, port ):
    return "prun -1 %s %d %d fs0.das2.cs.vu.nl %s %s -satin-closed" % (run_ibis, pno, port, solver, problem )

def report( msg ):
    print msg
    print >> lf, msg

report( "Solver: " + solver )
report( "Problem: " + problem )

for P in ProcNos:
    cmd = build_run_command( P, solver, problem, nameserverport )
    st = get_time_stamp()
    report( "---- P=%d start at %s ----" % (P, st) )
    report( "Command: " + cmd )
    f = os.popen( cmd )
    l = f.read().split( '\n' )
    f.close()
    for e in l:
        if e != '':
            report( e )
    st = get_time_stamp()
    report( "---- P=%d end at %s ----" % (P, st) )

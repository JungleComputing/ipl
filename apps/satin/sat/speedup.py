import os
import time
import popen2
import fcntl
import FCNTL
import select

ibisdir = "~/ibis"

run_ibis = ibisdir + "/bin/run_ibis"

logdir = "logs"

#problem = "examples/qg/qg6-12.cnf.gz"
problem = "examples/ais/ais10.cnf.gz"

solver = "DPLLSolver"

ProcNos = [ 1, 2, 4, 8, 16, 32 ]

nameserverport = 2001

def get_time_stamp():
    return time.strftime( "%Y-%m-%d-%H:%M:%S", time.localtime())


def makeNonBlocking( fd ):
    fl = fcntl.fcntl(fd, FCNTL.F_GETFL)
    try:
	fcntl.fcntl(fd, FCNTL.F_SETFL, fl | FCNTL.O_NDELAY)
    except AttributeError:
	fcntl.fcntl(fd, FCNTL.F_SETFL, fl | FCNTL.FNDELAY)
    
# Run the given command. Return a tuple with the exit code, the stdout text,
# and the stderr text.
def getCommandOutput( command ):
    child = popen2.Popen3(command, 1) # capture stdout and stderr from command
    child.tochild.close()             # don't need to talk to child
    outfile = child.fromchild 
    outfd = outfile.fileno()
    errfile = child.childerr
    errfd = errfile.fileno()
    makeNonBlocking( outfd )            # don't deadlock!
    makeNonBlocking( errfd )
    outdata = errdata = ''
    outeof = erreof = 0
    while 1:
	ready = select.select([outfd,errfd],[],[]) # wait for input
	if outfd in ready[0]:
	    outchunk = outfile.read()
	    if outchunk == '': outeof = 1
	    outdata = outdata + outchunk
	if errfd in ready[0]:
	    errchunk = errfile.read()
	    if errchunk == '': erreof = 1
	    errdata = errdata + errchunk
	if outeof and erreof: break
	select.select([],[],[],.2) # give a little time for buffers to fill
    err = child.wait()
    return (err, outdata, errdata)


def build_run_command( pno, solver, problem, port ):
    return "prun -1 %s %d %d fs0.das2.cs.vu.nl %s %s -satin-closed" % (run_ibis, pno, port, solver, problem )

def report( msg, lf = None ):
    print msg
    if( lf != None ):
        print >> lf, msg

def reportTrace( txt, label, lf = None ):
    if txt == '':
        return
    report( "---- start of " + label + " ----", lf )
    l = txt.split( '\n' )
    for e in l:
        if e != '':
            report( e, lf )
    report( "---- end of " + label + " ----", lf )

def reportRun( label, data, lf = None ):
    (exitcode, out, err) = data
    if exitcode != 0:
        print "Command failed: exit code %d" % exitcode
    reportTrace( out, label + " output stream", lf )
    reportTrace( err, label + " error stream", lf )

def reportedRun( P, lf = None ):
    cmd = build_run_command( P, solver, problem, nameserverport )
    report( "Command: " + cmd, lf )
    data = getCommandOutput( cmd )
    label = "P=%d" % P
    reportRun( label, data, lf )

def run():
    logfile = logdir + "/log-" + get_time_stamp()
    lf = open( logfile, "w" )
    report( "Solver: " + solver, lf )
    report( "Problem: " + problem, lf )

    for P in ProcNos:
        reportedRun( P, lf )

#reportedRun( "test", "ls" )
run()

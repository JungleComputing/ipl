import fcntl
import FCNTL
import getopt
import os
import popen2
import select
import string
import sys
import threading
import time
import types

ibisdir = "~/ibis"

run_ibis = os.path.join( ibisdir, "bin", "ibis-prun" )

logdir = "logs"

maxRunTime = "10:00"

verbose = 0

results = {}

nameserverport = 2001

# The timing result line starts with this string
timingTag = "ExecutionTime:"

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
    if verbose:
        print "Executing: " + command
    child = popen2.Popen3(command, 1) # capture stdout and stderr from command
    child.tochild.close()             # don't need to talk to child
    outfile = child.fromchild 
    outfd = outfile.fileno()
    errfile = child.childerr
    errfd = errfile.fileno()
    makeNonBlocking( outfd )            # don't deadlock!
    makeNonBlocking( errfd )
    errdata = ''
    outeof = erreof = 0
    outdata = 'Command: ' + command + '\n'
    while 1:
	ready = select.select([outfd,errfd],[],[]) # wait for input
	if outfd in ready[0]:
	    outchunk = outfile.read()
	    if outchunk == '': outeof = 1
	    outdata = outdata + outchunk
	if errfd in ready[0]:
            try:
                errchunk = errfile.read()
            except IOError, e:
                if e.errno != 11:
                    raise
            else:
                if errchunk == '': erreof = 1
                errdata = errdata + errchunk
	if outeof and erreof: break
	select.select([],[],[],.1) # give a little time for buffers to fill
    err = child.wait()
    return (err, outdata, errdata)

def build_run_command( pno, command, port ):
    return "prun -1 -t %s %s %d -ns-port %d -ns fs0.das2.cs.vu.nl %s" % (maxRunTime, run_ibis, pno, port, command)

def runP( command ):
    cmd = build_run_command( 1, command, nameserverport )
    print "Starting sequential run"
    data = getCommandOutput( cmd )
    print "Finished sequential run"
    return data

def report( msg, clients ):
    for f in clients:
        print >> f, msg

def reportTrace( txt, label, clients ):
    if txt == '':
        return
    report( "---- start of " + label + " ----", clients )
    l = txt.split( '\n' )
    for e in l:
        if e != '':
            report( e, clients )
    report( "---- end of " + label + " ----", clients )

def reportRun( label, data, clients ):
    (exitcode, out, err) = data
    if exitcode != 0:
        print "Command failed: exit code %d" % exitcode
    reportTrace( out, label + " output stream", clients )
    reportTrace( err, label + " error stream", clients )

# Given a line of the output, extract the execution time, or
# Null if it doesn't occur in this line.
def extractResultLine( l ):
    sz = len( timingTag )
    if l[0:sz] == timingTag:
       return l[sz:].strip()
    return None

# Given a results tuple, extract the execution time
def extractResult( data ):
    (exitcode, out, err) = data
    if exitcode != 0:
        # Run failed, no result.
        return None
    l = out.split( '\n' )
    for e in l:
       res = extractResultLine( e )
       if res != None:
           return res
    return None

def run( command, logfile ):
    if logfile == None:
        logfile = os.path.join( logdir, "log-" + get_time_stamp() + ".txt" )
    lf = open( logfile, "w" )
    logstreams = [lf]
    allstreams = [lf,sys.stdout]
    report( "Command: " + command, allstreams )
    report( "Logfile: " + logfile, allstreams )
    report( "Tag: '" + timingTag + "'", allstreams )

    res = runP( command )
    t = extractResult( res )
    report( "Time %s" % t, allstreams )
    reportRun( "Sequential", res, logstreams )
    lf.close()

def usage():
    print "Execute the given Ibis program on a single node of a parallel cluster."
    print "Usage: python seq.py [options] [program] [parameter...parameter]"
    print "The following options are supported:"
    print "--help\t\t\tShow this help text."
    print "-h\t\t\tShow this help text."
    print "--logdir [name]\t\tUse the specified log directory."
    print "--logfile [name]\tUse the specified log file."
    print "--port <number>\t\tUse the given nameserver port."
    print "--time <time>\t\tMaximal time per run <time> = [[hh:]mm:]ss"
    print "--verbose\t\tShow some progress information."
    print "-v\t\t\tShow some progress information."

def main():
    global ProcNos, nameserverport, timingTag, maxRunTime, verbose
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hv", ["help", "logfile=", "logdir=", "verbose", "port=", "time=", "tag="])
    except getopt.GetoptError:
        # print help information and exit:
        usage()
        sys.exit(2)
    logfile = None
    for o, a in opts:
        #print "Option [%s][%s]" % (o, a)
        if o in ("--procs", ):
            procSet = a
        if o in ("-v", "--verbose"):
            verbose = 1
        if o in ("-h", "--help"):
            usage()
            sys.exit()
        if o in ("--tag",):
            timingTag = a
        if o in ("--time",):
            maxRunTime = a
        if o in ("--logdir",):
            logdir = a
        if o in ("--port",):
            nameserverport = int(a)
        if o in ("--logfile",):
            logfile = a
    run( string.join( args, ' ' ), logfile )

if __name__ == "__main__":
    main()


package ibis.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to run a process and read its output in a separate thread.
 * Afterwards, the user can obtain the output or error output.
 * To run a command, the sequence is:
 * <br>
 * <pre>
 *     RunProcess p = new RunProcess("command ...");
 *     byte[] o = p.getStdout();
 *     byte[] e = p.getStderr();
 *     int status = p.getExitStatus();
 * </pre> 
 */
public final class RunProcess {
    private static final Runtime r = Runtime.getRuntime();

    /** Where standard output is collected. */
    private byte[] stdout = new byte[4096];

    /** Where standard error output is collected. */
    private byte[] stderr = new byte[4096];

    /** How much standard output is read. */
    private int ocount;

    /** How much standard error output is read. */
    private int ecount;

    /** Indicates that the reader thread is finished. */
    private boolean done;

    /** Indicates the exit status of the process. */
    private int exitstatus;

    /** The <code>Process</code> object for the command. */
    private Process p;

    /**
     * Separate thread that reads the output and error output of the
     * command.
     */
    private class Proc extends Thread {
	public void run() {
	    boolean must_read;
	    InputStream o = p.getInputStream();
	    InputStream e = p.getErrorStream();

	    do {
		must_read = false;

		if (ocount == stdout.length) {
		    byte[] b = new byte[2 * stdout.length];
		    System.arraycopy(stdout, 0, b, 0, stdout.length);
		    stdout = b;
		}
		int ro = 0;
		try {
		    ro = o.read(stdout, ocount, stdout.length - ocount);
		} catch(IOException ex) {
		    ro = -1;
		}
		if (ro != -1) {
		    ocount += ro;
		}

		if (ecount == stderr.length) {
		    byte[] b = new byte[2 * stderr.length];
		    System.arraycopy(stderr, 0, b, 0, stderr.length);
		    stderr = b;
		}
		int re = 0;
		try {
		    re = e.read(stderr, ecount, stderr.length - ecount);
		} catch(IOException ex) {
		    re = -1;
		}
		if (re != -1) {
		    ecount += re;
		}

		if (re >= 0 || ro >= 0) {
		    must_read = true;
		}

	    } while (must_read);
	    synchronized(p) {
		done = true;
		p.notifyAll();
	    }
	}
    }

    /**
     * Runs the command as specified.
     * Blocks until the command is finished.
     * @param command the specified command.
     */
    public RunProcess(String command) {

	ocount = 0;
	ecount = 0;
	exitstatus = -1;

	try {
	    p = r.exec(command);
	} catch(Exception e) {
	    return;
	}

	done = false;

	Proc reader = new Proc();
	reader.start();

	boolean interrupted = false;

	do {
	    try {
		interrupted = false;
		exitstatus = p.waitFor();
	    } catch(InterruptedException e) {
		interrupted = true;
	    }
	} while (interrupted);

	synchronized(p) {
	    while (! done) {
		try {
		    p.wait();
		} catch(InterruptedException e) {
		}
	    }
	}
    }

    /**
     * Returns the output buffer of the process.
     * @return the output buffer.
     */
    public byte[] getStdout() {
	byte b[] = new byte[ocount];
	System.arraycopy(stdout, 0, b, 0, ocount);
	return b;
    }

    /**
     * Returns the error output buffer of the process.
     * @return the error output buffer.
     */
    public byte[] getStderr() {
	byte b[] = new byte[ecount];
	System.arraycopy(stderr, 0, b, 0, ecount);
	return b;
    }

    /**
     * Returns the exit status of the process.
     * @return the exit status.
     */
    public int getExitStatus() {
	return exitstatus;
    }
}

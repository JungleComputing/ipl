/* $Id$ */

package ibis.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Utility to run a process and read its output in a separate thread.
 * Afterwards, the user can obtain the output or error output.
 * To run a command, the sequence is:
 * <br>
 * <pre>
 *     RunProcess p = new RunProcess("command", "arg1", ...);
 *     p.run();
 *     byte[] o = p.getStdout();
 *     byte[] e = p.getStderr();
 *     int status = p.getExitStatus();
 * </pre> 
 */
public final class RunProcess {
    
    private final ProcessBuilder builder;

    static class buf {
        public InputStream s;

        public byte[] buffer = new byte[4096];

        public int sz = 0;

        public boolean done = false;

        buf(InputStream s) {
            this.s = s;
        }

        buf(byte[] b) {
            this.buffer = b;
            this.sz = b.length;
            this.done = true;
        }
    }

    /** Indicates the exit status of the process. */
    private int exitStatus = -1;

    /** The <code>Process</code> object for the command. */
    private Process p = null;

    /** Collects stdout of process. */
    private buf proc_out;

    /** Collects stderr of process. */
    private buf proc_err;

    /**
     * Separate thread that reads the output and error output of the
     * command.
     */
    private static class Proc extends Thread {
        buf b;

        Proc(buf b) {
            this.b = b;
        }

        public void run() {
            boolean must_read;
            do {
                must_read = false;
                if (b.sz == b.buffer.length) {
                    byte[] newbuf = new byte[2 * b.buffer.length];
                    System.arraycopy(b.buffer, 0, newbuf, 0, b.buffer.length);
                    b.buffer = newbuf;
                }
                int ro = 0;
                try {
                    ro = b.s.read(b.buffer, b.sz, b.buffer.length - b.sz);
                } catch (IOException ex) {
                    ro = -1;
                }
                if (ro != -1) {
                    b.sz += ro;
                }

                if (ro >= 0) {
                    must_read = true;
                }

            } while (must_read);

            synchronized (b) {
                b.done = true;
                b.notifyAll();
            }
        }
    }

    /**
     * Creates a RunProcess object for the specified command.
     * @param command the specified command and arguments.
     */
    public RunProcess(String... command) {
        builder = new ProcessBuilder(command);
    }

    /**
     * Creates a RunProcess object for the specified command.
     * @param command the specified command and arguments.
     */
    public RunProcess(List<String> command) {
        builder = new ProcessBuilder(command);
    }
    
    /**
     * Runs the built command.
     * This method blocks until the command is finished, after
     * which exit status, output and error output can be obtained.
     */
    public void run() {
        try {
            p = builder.start();
        } catch (Exception e) {
            proc_err = new buf(("Could not execute cmd: " + builder.toString()).getBytes());
            return;
        }

        dealWithResult();
    }

    private void dealWithResult() {

        proc_out = new buf(p.getInputStream());
        proc_err = new buf(p.getErrorStream());

        Proc stdoutreader = new Proc(proc_out);
        Proc errorreader = new Proc(proc_err);
        stdoutreader.setDaemon(true);
        errorreader.setDaemon(true);
        stdoutreader.start();
        errorreader.start();

        boolean interrupted = false;

        do {
            try {
                interrupted = false;
                exitStatus = p.waitFor();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (interrupted);

        synchronized (proc_out) {
            while (!proc_out.done) {
                try {
                    proc_out.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        synchronized (proc_err) {
            while (!proc_err.done) {
                try {
                    proc_err.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns the output buffer of the process.
     * @return the output buffer.
     */
    public byte[] getStdout() {
        if (proc_out == null) {
            return new byte[0];
        }
        byte b[] = new byte[proc_out.sz];
        System.arraycopy(proc_out.buffer, 0, b, 0, proc_out.sz);
        return b;
    }

    /**
     * Returns the error output buffer of the process.
     * @return the error output buffer.
     */
    public byte[] getStderr() {
        if (proc_err == null) {
            return new byte[0];
        }
        byte b[] = new byte[proc_err.sz];
        System.arraycopy(proc_err.buffer, 0, b, 0, proc_err.sz);
        return b;
    }

    /**
     * Returns the exit status of the process.
     * @return the exit status.
     */
    public int getExitStatus() {
        return exitStatus;
    }
    
    /**
     * @see ProcessBuilder#command().
     */
    public List<String> command() {
        return builder.command();
    }
    
    /**
     * @see ProcessBuilder#command(List).
     */
    public RunProcess command(List<String> command) {
        builder.command(command);
        return this;
    }
    
    /**
     * @see ProcessBuilder#command(String...).
     */
    public RunProcess command(String... command) {
        builder.command(command);
        return this;
    }
    
    /**
     * @see ProcessBuilder#directory().
     */
    public File directory() {
        return builder.directory();
    }
    
    /**
     * @see ProcessBuilder#directory(File).
     */
    public ProcessBuilder directory(File directory) {
        return builder.directory(directory);
    }

    /**
     * @see ProcessBuilder#environment().
     */
    public Map<String, String> environment() {
        return builder.environment();
    }
    
    /**
     * @see ProcessBuilder#redirectErrorStream().
     */
    public boolean redirectErrorStream() {
        return builder.redirectErrorStream();
    }

    /**
     * @see ProcessBuilder#redirectErrorStream(boolean).
     */
    public RunProcess redirectErrorStream(boolean redirectErrorStream) {
        builder.redirectErrorStream(redirectErrorStream);
        return this;
    }
    
    /**
     * @see ProcessBuilder#start().
     */
    public Process start() throws IOException {
        return builder.start();
    }
}

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
    private int exitstatus;

    /** The <code>Process</code> object for the command. */
    private Process p;

    /** Collects stdout of process. */
    private buf proc_out;

    /** Collects stderr of process. */
    private buf proc_err;

    /**
     * Separate thread that reads the output and error output of the
     * command.
     */
    private class Proc extends Thread {
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
     * Runs the command as specified.
     * Blocks until the command is finished.
     * @param command the specified command.
     */
    public RunProcess(String command) {

        exitstatus = -1;

        try {
            p = r.exec(command);
        } catch (Exception e) {
            // Should not happen. At least there is a non-zero exit status.
            proc_err = new buf(("Could not execute command: " + command)
                    .getBytes());
            return;
        }

        dealWithResult();
    }

    /**
     * Runs the command as specified.
     * Blocks until the command is finished.
     * @param command the specified command and arguments.
     * @param env the environment.
     */
    public RunProcess(String[] command, String env[]) {

        exitstatus = -1;

        try {
            p = r.exec(command, env);
        } catch (Exception e) {
            // Should not happen. At least there is a non-zero exit status.
            String cmd = "";
            for (int i = 0; i < command.length; i++) {
                cmd = cmd + command[i] + " ";
            }
            proc_err = new buf(("Could not execute cmd: " + cmd).getBytes());
            return;
        }

        dealWithResult();
    }

    private void dealWithResult() {

        proc_out = new buf(p.getInputStream());
        proc_err = new buf(p.getErrorStream());

        Proc stdoutreader = new Proc(proc_out);
        Proc errorreader = new Proc(proc_err);
        stdoutreader.start();
        errorreader.start();

        boolean interrupted = false;

        do {
            try {
                interrupted = false;
                exitstatus = p.waitFor();
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
        byte b[] = new byte[proc_err.sz];
        System.arraycopy(proc_err.buffer, 0, b, 0, proc_err.sz);
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

package ibis.ipl.impl.messagePassing;

import java.io.IOException;

class InterruptCatcher extends Thread {

    /**
     * Copy the signal mnemonics from linux's /usr/include/bits/signal.h
     */

    static final int SIGHUP	= 1;	// Hangup (POSIX)
    static final int SIGINT	= 2;	// Interrupt (ANSI)
    static final int SIGQUIT	= 3;	// Quit (POSIX)
    static final int SIGILL	= 4;	// Illegal instruction (ANSI)
    static final int SIGTRAP	= 5;	// Trace trap (POSIX)
    static final int SIGABRT	= 6;	// Abort (ANSI)
    static final int SIGIOT	= 6;	// IOT trap (4.2 BSD)
    static final int SIGBUS	= 7;	// BUS error (4.2 BSD)
    static final int SIGFPE	= 8;	// Floating-point exception (ANSI)
    static final int SIGKILL	= 9;	// Kill, unblockable (POSIX)
    static final int SIGUSR1	= 10;	// User-defined signal 1 (POSIX)
    static final int SIGSEGV	= 11;	// Segmentation violation (ANSI)
    static final int SIGUSR2	= 12;	// User-defined signal 2 (POSIX)
    static final int SIGPIPE	= 13;	// Broken pipe (POSIX)
    static final int SIGALRM	= 14;	// Alarm clock (POSIX)
    static final int SIGTERM	= 15;	// Termination (ANSI)
    static final int SIGSTKFLT	= 16;	// Stack fault
    static final int SIGCHLD	= 17;	// Child status has changed (POSIX)
    static final int SIGCLD	= SIGCHLD; // Same as SIGCHLD (System V)
    static final int SIGCONT	= 18;	// Continue (POSIX)
    static final int SIGSTOP	= 19;	// Stop, unblockable (POSIX)
    static final int SIGTSTP	= 20;	// Keyboard stop (POSIX)
    static final int SIGTTIN	= 21;	// Background read from tty (POSIX)
    static final int SIGTTOU	= 22;	// Background write to tty (POSIX)
    static final int SIGURG	= 23;	// Urgent condition on socket (4.2 BSD)
    static final int SIGXCPU	= 24;	// CPU limit exceeded (4.2 BSD)
    static final int SIGXFSZ	= 25;	// File size limit exceeded (4.2 BSD)
    static final int SIGVTALRM	= 26;	// Virtual alarm clock (4.2 BSD)
    static final int SIGPROF	= 27;	// Profiling alarm clock (4.2 BSD)
    static final int SIGWINCH	= 28;	// Window size change (4.3 BSD, Sun)
    static final int SIGIO	= 29;	// I/O now possible (4.2 BSD)
    static final int SIGPOLL	= SIGIO; // Pollable event occurred (System V)
    static final int SIGPWR	= 30;	// Power failure restart (System V)
    static final int SIGSYS	= 31;	// Bad system call
    static final int SIGUNUSED	= 31;

    static final int _NSIG	= 64;	// Biggest signal number + 1
					// (including real-time signals)


    private final static int pollsPerSignal = 4;

    private native boolean supported();
    private native void registerHandler();
    private native void waitForSignal();

    private int signo;

    InterruptCatcher() {
	this(SIGIO);
    }

    InterruptCatcher(int signo) {
	this.signo = signo;
	setDaemon(true);
    }

    public void run() {
	if (! supported()) {
	    System.err.println("No interrupt support");
	    return;
	}

	registerHandler();

	while (true) {
	    try {
		Ibis.myIbis.lock();
		for (int i = 0; i < pollsPerSignal; i++) {
		    Ibis.myIbis.pollLocked();
		}
		Ibis.myIbis.unlock();

		waitForSignal();
	    } catch (IOException e) {
		System.err.println("From interrupt handler: exception " + e);
	    }
	}
    }

}

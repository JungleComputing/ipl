package ibis.impl.messagePassing;

import ibis.ipl.IbisConfigurationException;
import ibis.util.ConditionVariable;

/**
 * Provide multi-locks.
 * If multiple send ports want to send to receive ports on one other CPU,
 * flow control of the underlying message passing system requires that
 * these messages are serialized, not interleaved.
 * Solve this by locking the channel(s) to the cpu that underly the
 * Ibis message.
 * In the case of multicast, this is a special problem. because all
 * channels must be locked at the same time.
 */

class SendSerializer {

    private boolean[] locked;

    private int[] waiting;

    private ConditionVariable cv;

    SendSerializer() {
        if (Ibis.myIbis.nrCpus == 0) {
            throw new IbisConfigurationException(
                    "Must set nrCpus before creating SendSerializer");
        }
        cv = Ibis.myIbis.createCV();
        locked = new boolean[Ibis.myIbis.nrCpus];
        waiting = new int[Ibis.myIbis.nrCpus];
    }

    private void lock(int i) {
        locked[i] = true;
    }

    private boolean unlock(int i) {
        if (i == Ibis.myIbis.myCpu) {
            // No flow control at home
            return false;
        }

        locked[i] = false;
        return (waiting[i] > 0);
    }

    private boolean tryLock(int i) {
        if (i == Ibis.myIbis.myCpu) {
            // No flow control at home
            return true;
        }

        if (locked[i]) {
            return false;
        }
        locked[i] = true;
        return true;
    }

    private void registerWaiting(int i) {
        waiting[i]++;
    }

    private void unregisterWaiting(int i) {
        waiting[i]--;
    }

    void lockAll(int[] cpu) {
        Ibis.myIbis.checkLockOwned();
        int lockcounter;
        do {
            lockcounter = 0;
            for (int i = 0; i < cpu.length; i++) {
                if (tryLock(cpu[i])) {
                    lockcounter++;
                } else {
                    /* Sorry, cannot lock all. Release the locks I already
                     * got, and try again */
                    boolean mustSignal = false;
                    for (int j = 0; j < i; j++) {
                        mustSignal |= unlock(cpu[j]);
                    }
                    for (int j = 0; j < cpu.length; j++) {
                        registerWaiting(cpu[j]);
                    }
                    if (mustSignal) {
                        cv.cv_bcast();
                    }

                    try {
                        cv.cv_wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }

                    for (int j = 0; j < cpu.length; j++) {
                        unregisterWaiting(cpu[j]);
                    }
                    break;
                }
            }
        } while (lockcounter < cpu.length);
    }

    void unlockAll(int[] cpu) {
        Ibis.myIbis.checkLockOwned();
        boolean mustSignal = false;
        for (int i = 0; i < cpu.length; i++) {
            mustSignal |= unlock(cpu[i]);
        }
        if (mustSignal) {
            cv.cv_bcast();
        }
    }

}

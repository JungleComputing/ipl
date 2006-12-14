/* $Id$ */

package ibis.impl.messagePassing;

/**
 * Class to synchronize between connecter and connected
 */
class ConnectAcker extends Syncer {

    private boolean accepted;

    private int acks = 1;

    /**
     * Set initial value before synchronization and clear accepted condition
     *
     * @param acks initial value for <code>acks</code>.
     */
    public void setAcks(int acks) {
        this.acks = acks;
        this.accepted = false;
    }

    /**
     * Test if synchronization has completed successfully.
     *
     * @return true if <code>acks</code> equals 0.
     */
    public boolean satisfied() {
        return acks == 0;
    }

    /**
     * Test accepted condition
     *
     * @return true if the accepted condition has been set
     */
    public boolean accepted() {
        return accepted;
    }

    /**
     * Signal condition by decrementing the <code>acks</code> value.
     * Doesn't touch the accepted field.
     */
    public void signal() {
        if (acks <= 0) {
            throw new Error(this + ": wakeup but acks " + acks);
        }
        --acks;
        wakeup();
    }

    /**
     * Signal condition by setting the accepted field
     * Doesn't touch the <code>acks</code> field.
     */
    public void signal(boolean accept) {
        this.accepted = accept;
        signal();
    }

}


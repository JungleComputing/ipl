package ibis.ipl.impl.net;

import java.io.IOException;

public interface NetPollInterruptible {

    /**
     * NetInput subclasses that implement this interface
     * support a blocking poll that is interruptible via the method
     * interruptPoll().
     * The implementation of this method may be costly.
     * A thread in a (blocking) poll may throw an InterruptedIOException
     * upon an interruptPoll().
     */
    public void interruptPoll() throws IOException;

    /**
     * Blocking polls are interruptible after this method has been called.
     * Polls are interruptible when they start <B>after</B>
     * this call has been performed.
     * It is implementation dependent if poll interruptibility can be switched
     * on again after having been switched off using clearInterruptible.
     * If not, this method will throw an IllegalArgumentException.
     *
     * @param on enable/disable interruptibility of the poll
     */
    public void setInterruptible() throws IOException;

    /**
     * Blocking polls are no longer interruptible after this method has been
     * called.
     *
     * @param inputUpcall if this parameter is nonnull, message receipt will
     * 			be done using this upcall after switching off the
     * 			interruptibility has been handled.
     */
    public void clearInterruptible(NetInputUpcall inputUpcall) throws IOException;

}

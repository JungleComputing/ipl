package ibis.impl.net;

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
     * If this method is invoked and the implementation supports it, any upcall
     * threads are stopped and the receives must be done in downcall fashion.
     */
    public void setInterruptible() throws IOException;

    /**
     * Blocking polls are no longer interruptible after this method has been
     * called.
     * If parameter inputUpcall is nonnull, an upcall thread is started and
     * receives are done in upcall fashion.
     *
     * @param inputUpcall if this parameter is nonnull, message receipt will
     * 			be done using this upcall after switching off the
     * 			interruptibility has been handled.
     */
    public void clearInterruptible(NetInputUpcall inputUpcall) throws IOException;

}

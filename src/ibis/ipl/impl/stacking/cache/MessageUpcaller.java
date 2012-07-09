package ibis.ipl.impl.stacking.cache;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.cache.CacheReadMessage.CacheReadUpcallMessage;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import java.io.IOException;
import java.util.logging.Level;

/**
 * This class handles message upcalls.
 */
public final class MessageUpcaller implements MessageUpcall {

    /*
     * The user defined message upcaller.
     */
    MessageUpcall upcaller;
    CacheReceivePort port;
    static boolean wasLastPart = true;
    static CacheReadMessage currentLogicalMsg;
    public static final Object currentLogicalMsgLock = new Object();
    static boolean currentLogicalMsgFinished;
    /*
     * If the current true message has been or not emptied.
     */
    public boolean messageDepleted;

    public MessageUpcaller(MessageUpcall upcaller, CacheReceivePort port) {
        this.upcaller = upcaller;
        this.port = port;
    }

    @Override
    public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
        messageDepleted = false;
        boolean isLastPart = m.readBoolean();

        CacheManager.log.log(Level.INFO, "\nGot message upcall. isLastPart={0}\n", isLastPart);

        // this is a logically new message.
        if (wasLastPart) {
            currentLogicalMsg = new CacheReadUpcallMessage(m, port);
            currentLogicalMsgFinished = false;
        }

        /*
         * Feed the buffer of the DataInputStream with the data from this
         * message. the format of the message is: (bufSize, byte[bufSize]
         * buffer);
         */
        currentLogicalMsg.offerToBuffer(isLastPart, m);

        if (wasLastPart) {
            wasLastPart = isLastPart;
            // this is a logically new message.
            upcaller.upcall(currentLogicalMsg);
            synchronized (currentLogicalMsgLock) {
                currentLogicalMsgFinished = true;
                currentLogicalMsg.finish();
                currentLogicalMsgLock.notify();
            }
        } else {
            wasLastPart = isLastPart;
        }

        /*
         * Block until all data from m has been removed or until the user upcall
         * is done.
         */
        synchronized (currentLogicalMsgLock) {
            while (!messageDepleted && !currentLogicalMsgFinished) {
                try {
                    CacheManager.log.log(Level.INFO, "Waiting on current "
                            + "streamed message to be depleted or"
                            + "current logical message to be finished.");
                    currentLogicalMsgLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
        CacheManager.log.log(Level.INFO, "Finishing 1 message upcall.\n");
        
        // now `m` will be finished at the exit from this method.
    }
}

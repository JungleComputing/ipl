/* $Id$ */

package ibis.impl.nio;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class used as a single send/receive thread for an entire NioIbis instance
 */
final class SendReceiveThread implements Runnable, Config {

    static final int INITIAL_ARRAY_SIZE = 8;

    private ArrayList pendingChannels = new ArrayList();

    private ArrayList pendingAttachments = new ArrayList();

    private SelectionKey[] readyWriteKeys;

    private int nrOfReadyWriteKeys;

    private SelectionKey[] readyReadKeys;

    private int nrOfReadyReadKeys;

    private Selector selector;

    private boolean exit = false;

    SendReceiveThread() throws IOException {
        selector = Selector.open();
        ThreadPool.createNew(this);

        readyWriteKeys = new SelectionKey[INITIAL_ARRAY_SIZE];
        readyReadKeys = new SelectionKey[INITIAL_ARRAY_SIZE];
    }

    /**
     * Registers the given channel with our selector.
     *
     * @return The SelectionKey representing the registration,
     *	       with the given attachment attached to it.
     */
    synchronized SelectionKey register(SelectableChannel channel,
            Object attachment) throws IOException {
        SelectionKey key = null;

        pendingChannels.add(channel);
        pendingAttachments.add(attachment);

        channel.configureBlocking(false);

        selector.wakeup();

        while (key == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                //IGNORE
            }
            key = channel.keyFor(selector);
        }
        return key;
    }

    void registerPendingChannels() {
        SelectableChannel channel;
        SelectionKey key;

        if (pendingChannels.size() == 0) {
            return;
        }

        if (DEBUG) {
            Debug.message("channels", this, "registering "
                    + pendingChannels.size() + " channels");
        }

        for (int i = 0; i < pendingChannels.size(); i++) {
            channel = (SelectableChannel) pendingChannels.get(i);
            try {
                key = channel.register(selector, 0);
                key.attach(pendingAttachments.get(i));
            } catch (IOException e) {
                //IGNORE
            }
        }

        pendingChannels.clear();
        pendingAttachments.clear();

        notifyAll();
    }

    /**
     * signals a connection is ready for writing data
     */
    synchronized void enableWriting(SelectionKey key) {
        if (DEBUG) {
            Debug.message("channels", this, "queueing write enable");
        }

        if (nrOfReadyWriteKeys == readyWriteKeys.length) {
            SelectionKey[] newKeys;
            newKeys = new SelectionKey[readyWriteKeys.length * 2];
            for (int i = 0; i < readyWriteKeys.length; i++) {
                newKeys[i] = readyWriteKeys[i];
            }
            readyWriteKeys = newKeys;
        }
        readyWriteKeys[nrOfReadyWriteKeys] = key;
        nrOfReadyWriteKeys++;

        selector.wakeup();
    }

    /**
     * signals a connection is ready to read data
     */
    synchronized void enableReading(SelectionKey key) {
        if (DEBUG) {
            Debug.message("channels", this, "queueing read enable");
        }

        if (nrOfReadyReadKeys == readyReadKeys.length) {
            SelectionKey[] newKeys;
            newKeys = new SelectionKey[readyReadKeys.length * 2];
            for (int i = 0; i < readyReadKeys.length; i++) {
                newKeys[i] = readyReadKeys[i];
            }
            readyReadKeys = newKeys;
        }
        readyReadKeys[nrOfReadyReadKeys] = key;
        nrOfReadyReadKeys++;

        selector.wakeup();
    }

    void handlePendingKeys() {
        if (DEBUG) {
            if (nrOfReadyWriteKeys != 0 || nrOfReadyReadKeys != 0) {
                Debug.message("channels", this, "enabling "
                        + nrOfReadyWriteKeys + " write keys and "
                        + nrOfReadyReadKeys + " read keys");
            }
        }

        for (int i = 0; i < nrOfReadyWriteKeys; i++) {
            try {
                readyWriteKeys[i].interestOps(SelectionKey.OP_WRITE);
            } catch (CancelledKeyException e) {
                //Channels was closed/lost, ignore
            }
            readyWriteKeys[i] = null;
        }
        nrOfReadyWriteKeys = 0;

        for (int i = 0; i < nrOfReadyReadKeys; i++) {
            try {
                readyReadKeys[i].interestOps(SelectionKey.OP_READ);
            } catch (CancelledKeyException e) {
                //Channels was closed/lost, ignore
            }
            readyReadKeys[i] = null;
        }
        nrOfReadyReadKeys = 0;
    }

    private void send(SelectionKey key) {
        ThreadNioAccumulatorConnection out;

        out = (ThreadNioAccumulatorConnection) key.attachment();

        out.threadSend();
    }

    private void receive(SelectionKey key) {
        ThreadNioDissipator in;

        in = (ThreadNioDissipator) key.attachment();

        in.read();
    }

    /**
     * Stops the send/receive Thread
     */
    synchronized void quit() {
        exit = true;
    }

    public void run() {
        Iterator keys;
        SelectionKey key;

        Thread.currentThread().setName("send/receive thread");

        //try to add some importance to this thread
        try {
            int max = Thread.currentThread().getThreadGroup().getMaxPriority();
            int current = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Math.min(max, (current + 1)));
        } catch (Exception e) {
            //IGNORE
        }

        while (true) {
            if (DEBUG) {
                Debug.enter("channels", this, "looking for work");
            }

            synchronized (this) {
                if (exit) {
                    if (DEBUG) {
                        Debug.exit("channels", this, "done looking for work");
                    }
                    return;
                }
                registerPendingChannels();
                handlePendingKeys();
            }

            if (DEBUG) {
                Debug.message("channels", this, "doing a select on "
                        + selector.keys().size() + " channels");
            }

            try {
                selector.select();
            } catch (IOException e) {
                if (WARNINGS) {
                    System.err.println("ibis.impl.nio.SendReceiveThread.run():"
                            + " select failed with exception: " + e);
                    e.printStackTrace(System.err);
                }
                //IGNORE
            } catch (CancelledKeyException e) {
                //INGORE
            }

            if (DEBUG) {
                Debug.message("channels", this, "selected "
                        + selector.selectedKeys().size() + " channel(s)");
            }

            keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                key = (SelectionKey) keys.next();

                if (key.attachment() == null) {
                    continue; // skip this key, nothing attached to it
                }

                try {
                    if (key.isWritable()) {
                        send(key);
                    }
                    if (key.isReadable()) {
                        receive(key);
                    }
                } catch (CancelledKeyException e) {
                    //key was cancelled or channel was closed
                    //skip this key
                }
            }
            selector.selectedKeys().clear();

            if (DEBUG) {
                Debug.exit("channels", this, "done");
            }

        }
    }
}
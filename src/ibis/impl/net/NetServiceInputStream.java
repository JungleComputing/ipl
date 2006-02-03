/* $Id$ */

package ibis.impl.net;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provide a multiplexed input sub-stream over the socket input stream.
 */
public class NetServiceInputStream extends InputStream {

    private static final boolean DEBUG = false;

    /**
     * Provide a double-linked list of incoming buffers.
     */
    private static class BufferList {

        /**
         * Reference the next buffer list element.
         */
        BufferList next = null;

        /**
         * Reference the list element's buffer.
         */
        byte[] buf = null;
    }

    /**
     * Reference the buffer list head.
     */
    private BufferList first = null;

    /**
     * Reference the buffer list tail.
     */
    private BufferList last = null;

    /**
     * Set to true once the stream is closed.
     *
     * The stream cannot be re-opened after having been closed (same
     * semantics as a socket stream).
     */
    private boolean closed = false;

    /**
     * Store the packet identifier.
     */
    private int id = -1;

    /**
     * Store the total number of bytes available in the
     * incoming buffer list.
     */
    private int avail = 0;

    /**
     * Store the reading offset in the current buffer.
     */
    private int offset = 0;

    /**
     * A popUp handler. If one is registered, spawn a popup thread
     * for each incoming message. Else, notify some pending handler
     * thread.
     */
    private NetServicePopupThread popUp = null;

    private boolean handlingPopup = false;

    /**
     * Construct an incoming sub-stream.
     *
     * The {@link #id} value must be unique among this
     * service link incoming sub-streams.
     *
     * @param popUp if non<code>null</code> create a popUp thread for
     * 		each incoming message. Else, notify the receiver
     * 		thread.
     * @param id the sub-stream packets id.
     */
    public NetServiceInputStream(NetServicePopupThread popUp, int id) {
        this.id = id;
        this.popUp = popUp;
    }

    /**
     * Construct an incoming sub-stream.
     *
     * The {@link #id} value must be unique among this
     * service link incoming sub-streams.
     *
     * @param id the sub-stream packets id.
     */
    public NetServiceInputStream(int id) {
        this(null, id);
    }

    /**
     * Register a popup for this InputStream.
     *
     * @param popUp if non<code>null</code> create a popUp thread for
     * 		each incoming message. Else, notify the receiver
     * 		thread.
     */
    public synchronized void registerPopup(NetServicePopupThread popUp) {
        this.popUp = popUp;
        if (avail > 0) {
            spawnPopup();
        }
    }

    private synchronized boolean popupsFinished() {
        if (avail > 0) {
            return false;
        }

        handlingPopup = false;
        notifyAll();

        return true;
    }

    private void spawnPopup() {
        handlingPopup = true;
        Runnable r = new Runnable() {
            public void run() {
                if (DEBUG) {
                    System.err.println(this + ": Start popup handler");
                }
                do {
                    if (DEBUG) {
                        System.err.println(this + ": Start popup message");
                    }
                    try {
                        popUp.callBack();
                    } catch (IOException e) {
                        System.err.println(NetServiceInputStream.this
                                + ": Popup thread gets " + e);
                    }
                    if (DEBUG) {
                        System.err.println(this + ": End popup message");
                    }
                } while (!popupsFinished());
                if (DEBUG) {
                    System.err.println(this + ": End popup handler");
                }
            }
        };

        ThreadPool.createNew(r, popUp.getName());
        // new Thread(r, "popup-" + this).start();
    }

    /**
     * Called by the listenThread of {@link NetServiceLink}
     * to add a block of bytes to the incoming buffer list.
     *
     * @param b the byte block to add to the list.
     */
    protected synchronized void addBuffer(byte[] b) {
        BufferList bl = new BufferList();
        bl.buf = b;

        if (first == null) {
            first = bl;
        } else {
            last.next = bl;
        }
        last = bl;

        avail += bl.buf.length;
        if (DEBUG) {
            if (popUp != null) {
                System.err.println(this + ": addBuffer(" + bl.buf.length
                        + ") popUp " + popUp + " handlingPopup "
                        + handlingPopup);
            }
        }

        if (popUp != null && !handlingPopup) {
            spawnPopup();

        } else {
            notifyAll();
        }
    }

    /**
     * Return the number of bytes immediately available.
     *
     * The value returned is the value of the {@link #avail} attribute.
     *
     * @return the number of bytes immediately availables.
     * @exception IOException to conform to the {@link InputStream} definition.
     */
    public synchronized int available() throws IOException {
        return avail;
    }

    /**
     * Close the stream.
     *
     * The stream cannot be re-opened afterwards. Any unread data is lost.
     */
    public synchronized void close() throws IOException {
        closed = true;
        notifyAll();
    }

    /**
     * Return false.
     */
    public boolean markSupported() {
        return false;
    }

    public boolean closed() {
        return closed;
    }

    /**
     * Switch to the next block in the incoming buffer list.
     */
    private void nextBlock() {
        offset = 0;
        if (first.next == null) {
            first = null;
            last = null;
        } else {
            BufferList temp = first.next;
            first.next = null;
            first = temp;
        }
    }

    public synchronized int read() throws IOException {
        if (closed && avail == 0) {
            return -1;
        }

        int result = 0;

        if (avail == 0) {
            try {
                wait();
                if (closed) {
                    return -1;
                }
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e);
            }
        }

        if (avail > 0) {
            result = 0xFF & first.buf[offset++];
            avail--;
        }

        if (offset == first.buf.length) {
            nextBlock();
        }

        return result;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public synchronized int read(byte[] buf, int off, int len)
            throws IOException {
        if (closed && avail == 0) {
            return -1;
        }

        int result = 0;

        while (len > 0) {
            if (avail == 0) {
                if (closed || result > 0) {
                    break;
                }

                try {
                    wait();
                    if (closed) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new InterruptedIOException(e);
                }
            }

            int copylength = Math.min(len, first.buf.length - offset);
            System.arraycopy(first.buf, offset, buf, off, copylength);
            result += copylength;
            offset += copylength;
            off += copylength;
            len -= copylength;
            avail -= copylength;

            if (offset == first.buf.length) {
                nextBlock();
            }
        }

        return result;
    }
}

/* $Id$ */

package ibis.connect.NIOParallelStreams;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.ConnectionProperties;
import ibis.connect.IbisSocket;
import ibis.connect.IbisSocketFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class NIOParallelStreamsSocket extends IbisSocket {
    static Logger logger = Logger.getLogger(NIOParallelStreamsSocket.class
            .getName());

    public static final int BUF_SIZE = 128 * 1024;

    public static final int HEADER_SIZE = 8; // two ints: blockno and blocksize
    
    public static final int DEFAULT_NUM_WAYS = 4;

    public static final int DEFAULT_BLOCK_SIZE = (128 * 1024) - HEADER_SIZE;

    private static int portcounter = 1;

    private NIOParallelStreamsInputStream in = null;

    private NIOParallelStreamsOutputStream out = null;

    private int portno = -1;

    private int localportno = -1;

    private int numWays = 1;

    private int blockSize = DEFAULT_BLOCK_SIZE;

    private Socket[] sockets = null;

    private ChannelInputStream[] ins = null;

    private ChannelOutputStream[] outs = null;

    private int sendBlock = 0;

    private int recvBlock = 0;

    private Map props;

    private boolean hint = false;

    // NIO stuff
    //    private ByteBuffer[] sendBufs;

    private Selector sendSelector;

    private SelectionKey[] sendKey;

    private Selector recvSelector;

    private SelectionKey[] recvKey;

    /*
     * this array contains the recv block number that is currently available in
     * the corresponding channel
     */
    private int[] blockNos;

    /*
     * this array contains the size of the block that is available in the corresponding channel
     */
    private int[] blockSizes;

    private Set sendReadySet;

    private Iterator sendReadyIterator;

    private Set recvReadySet;

    private Iterator recvReadyIterator;

    private boolean[] sendInProgress;

    /*
     * End-users are not supposed to call this constructor. They should use the
     * socket factory instead.
     */
    protected NIOParallelStreamsSocket(InputStream ctrlIs, OutputStream ctrlOs,
            boolean hint, Map p) throws IOException {
        super(p);
        String snw = (String) p.get(ConnectionProperties.PAR_NUMWAYS);
        if (snw != null) {
            numWays = Integer.parseInt(snw);
        } else {
            logger.warn(
                    "using parallel streams, but numways property not set, using default");
            numWays = DEFAULT_NUM_WAYS;
        }
        String sbs = (String) p.get(ConnectionProperties.PAR_BLOCKSIZE);
        blockSize = NIOParallelStreamsSocket.DEFAULT_BLOCK_SIZE;
        if (sbs != null) {
            blockSize = Integer.parseInt(sbs);
        }

        System.err.println("# NIOParallelStreams: building link- numWays = "
                + numWays + "; blockSize = " + blockSize);
        sockets = new Socket[numWays];
        ins = new ChannelInputStream[numWays];
        outs = new ChannelOutputStream[numWays];
        this.props = p;

        sendInProgress = new boolean[numWays];

        portno = connect(ctrlIs, ctrlOs, hint, localportno);
        in = new NIOParallelStreamsInputStream(this);
        out = new NIOParallelStreamsOutputStream(this);

        // Create a Selector, register all Channels
        sendSelector = Selector.open();
        sendKey = new SelectionKey[numWays];

        for (int i = 0; i < numWays; i++) {
            sockets[i].getChannel().configureBlocking(false);
            sendKey[i] = sockets[i].getChannel().register(sendSelector,
                    SelectionKey.OP_WRITE);
        }

        // Create a Selector, register all Channels
        recvSelector = Selector.open();
        recvKey = new SelectionKey[numWays];

        for (int i = 0; i < numWays; i++) {
            recvKey[i] = sockets[i].getChannel().register(recvSelector,
                    SelectionKey.OP_READ);
        }

        blockNos = new int[numWays];
        blockSizes = new int[numWays];
        for (int i = 0; i < numWays; i++) {
            blockNos[i] = -1;
            blockSizes[i] = -1;
        }

        synchronized (NIOParallelStreamsSocket.class) {
            localportno = portcounter++;
        }
    }

    private int connect(InputStream in, OutputStream out, boolean hnt,
            int portno) throws IOException {
        int i;

        DataOutputStream os = new DataOutputStream(out);
        os.writeInt(numWays);
        os.writeInt(portno);
        os.flush();

        DataInputStream is = new DataInputStream(in);
        int rNumWays = is.readInt();
        int rport = is.readInt();

        logger.info("NIOPS: received properties from peer.");

        if (rNumWays != numWays) {
            throw new Error(
                    "NIOParallelStreams: cannot connect- localNumWays = "
                            + numWays + "; remoteNumWays = " + rNumWays);
        }

        BrokeredSocketFactory f = IbisSocketFactory.getFactory()
                .getParallelStreamsBaseType();

        hint = hnt;

        logger.info("NIOPS: connecting, numWays = " + numWays + " (hint="
                + hint + ")");

        for (i = 0; i < numWays; i++) {
            out.flush();
            Socket s = f.createBrokeredSocket(in, out, hint, props);
            sockets[i] = s;
            ins[i] = new ChannelInputStream(s.getChannel(), i);
            outs[i] = new ChannelOutputStream(s.getChannel(), i);
        }
        return rport;
    }

    public void setReceiveBufferSize(int n) throws SocketException {
        int recvBuf = 0;
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setReceiveBufferSize(n);
                recvBuf += sockets[i].getReceiveBufferSize();
            }
        }
        logger.info("NIOPS: total recv buffer size is: " + recvBuf
                + " avg per stream = " + (recvBuf / numWays));
    }

    public void setSendBufferSize(int n) throws SocketException {
        int sendBuf = 0;
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setSendBufferSize(n);
                sendBuf += sockets[i].getReceiveBufferSize();
            }
        }
        logger.info("NIOPS: total send buffer size is: " + sendBuf
                + " avg per stream = " + (sendBuf / numWays));
    }

    int available() throws IOException {
        return -1;
    }

    private int getChannelNumber(SelectionKey key, SelectionKey[] set) {
        for (int j = 0; j < numWays; j++) {
            if (set[j].equals(key)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("getChannelNumber: channel " + j
                            + " is ready");
                }
                return j;
            }
        }
        throw new Error("Could not find key");
    }

    /**
     * Returns a channel that is ready to accept data. If no channel is
     * currently ready, this method blocks.
     */
    private int sendSelect() throws IOException {
        while (true) {
            if (sendReadySet == null) {
                sendSelector.select();
                sendReadySet = sendSelector.selectedKeys();
                sendReadyIterator = sendReadySet.iterator();
            }
            SelectionKey key = null;

            while (sendReadyIterator.hasNext()) {
                key = (SelectionKey) sendReadyIterator.next();
                sendReadyIterator.remove();

                if (key.isWritable()) {
                    return getChannelNumber(key, sendKey);
                }
            }
            sendReadySet = null;
        }
    }

    private int checkForAlreadyReadChannel(int blockNo) throws IOException {
        // we might already have the channel number
        for (int i = 0; i < numWays; i++) {
            if (blockNos[i] == blockNo) {
                if (blockSizes[i] != -1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("recvSelect: channel " + i
                                + " was already waiting");
                    }
                    return i;
                } else {
                    blockSizes[i] = ins[i].readInt();
                    if (blockSizes[i] != -1) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("recvSelect: channel " + i
                                    + " was already waiting");
                        }
                        return i;
                    } else {
                        return -1;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Returns a channel that is ready to produce data. If no channel is
     * currently ready, this method blocks.
     */
    private int recvSelect(int blockNo) throws IOException {

        int res = checkForAlreadyReadChannel(blockNo);
        if (res >= 0)
            return res;

        while (true) {
            if (recvReadySet == null) {
                recvSelector.select();
                recvReadySet = recvSelector.selectedKeys();
                recvReadyIterator = recvReadySet.iterator();
            }

            SelectionKey key = null;

            while (recvReadyIterator.hasNext()) {
                key = (SelectionKey) recvReadyIterator.next();
                recvReadyIterator.remove();

                if (key.isReadable()) {
                    int channelIndex = getChannelNumber(key, recvKey);

                    if (blockNos[channelIndex] == -1
                            || blockSizes[channelIndex] == -1) {
                        // ok we have a readable channel, read blockNo to see if
                        // it is the right one
                        if (blockNos[channelIndex] == -1) {
                            blockNos[channelIndex] = ins[channelIndex]
                                    .readInt();
                        }
                        if (blockNos[channelIndex] != -1) {
                            blockSizes[channelIndex] = ins[channelIndex]
                                    .readInt();
                            if (blockSizes[channelIndex] != -1) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("recvSelect: channel "
                                            + channelIndex
                                            + " is ready, block = "
                                            + blockNos[channelIndex]
                                            + " size = "
                                            + blockSizes[channelIndex]);
                                }
                                if (blockNos[channelIndex] == blockNo) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("recvSelect: channel "
                                                + channelIndex
                                                + " just became ready");
                                    }
                                    return channelIndex;
                                }
                            } else {
                                if (logger.isDebugEnabled()) {
                                    logger
                                            .debug("selected but could not read block size");
                                }
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger
                                        .debug("selected but could not read block no");
                            }
                        }
                    }
                    // not the right one.
                }
            }

            recvReadySet = null;
        }
    }

    int recv(byte[] b, int off, int len) throws IOException {
        int channel = recvSelect(recvBlock);
        ChannelInputStream in = ins[channel];

        // block nr and blocksize have already been read, read data itself
        int blockSize = blockSizes[channel];

        if (blockSize > len) {
            if (logger.isDebugEnabled()) {
                logger.debug("recv: reading " + len + " bytes, but block is "
                        + blockSize + " bytes, " + (blockSize - len)
                        + " bytes leftover");
            }
            blockSizes[channel] = blockSize - len;

        } else if (blockSize < len) {
            if (logger.isDebugEnabled()) {
                logger.debug("recv: reading " + len
                        + " bytes, but block is only " + blockSize
                        + " bytes, reading less");
            }
            len = blockSize;
            blockNos[channel] = -1;
            blockSizes[channel] = 0;
            recvBlock++;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("recv: reading " + len + " bytes, exact fit");
            }
            blockNos[channel] = -1;
            blockSizes[channel] = 0;
            recvBlock++;
        }

        return in.read(b, off, len);
    }

    void send(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            throw new Error("send of length 0");
        }

        int remaining = len;
        int sent = 0;
        while (remaining != 0) {
            int toWrite = Math.min(remaining, BUF_SIZE - HEADER_SIZE);
            int chunkOff = off + sent;
            sendChunk(b, chunkOff, toWrite);
            sent += toWrite;
            remaining -= toWrite;
        }
    }

    // contract: when this call returns, data has been copied into a buffer.
    void sendChunk(byte[] b, int off, int len) throws IOException {
        while (true) {
            int channel = sendSelect();

            if (logger.isDebugEnabled()) {
                logger.debug("sendChunk: sending " + len + " bytes to channel "
                        + channel + ", block = " + sendBlock);
            }

            ChannelOutputStream o = outs[channel];
            if (!sendInProgress[channel]) {
                o.setBuf(sendBlock, b, off, len);
                if (o.write())
                    sendInProgress[channel] = false;
                else
                    sendInProgress[channel] = true;
                sendBlock++;
                return;
            } else {
                if (o.write())
                    sendInProgress[channel] = false;
            }
        }
    }

    public void shutdownInput() throws IOException {
        logger.info("NIOPS: shutdownInput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownInput();
        }
    }

    public void shutdownOutput() throws IOException {
        logger.info("NIOPS: shutdownOutput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownOutput();
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        logger.info("NIOPS: setTcpNoDelay");
        System.err.println("NIOPS: setTcpNoDelay");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setTcpNoDelay(on);
        }
    }

    public void setSoTimeout(int t) throws SocketException {
        logger.info("NIOPS: setSoTimeout");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setSoTimeout(t);
        }
    }

    public String toString() {
        String result = "NIOParallelStreams Socket ";
        result += "(numWays = " + numWays + ")";
        return result;
    }

    public void close() throws IOException {
        flush();
        logger.info("NIOPS: closing PS, numWays = " + numWays + ", hint = "
                + hint);
        for (int i = 0; i < numWays; i++) {
            if (ins[i] != null)
                ins[i].close();
            ins[i] = null;
            if (outs[i] != null)
                outs[i].close();
            outs[i] = null;
            if (sockets[i] != null)
                sockets[i].close();
            sockets[i] = null;
        }
        in = null;
        out = null;
        portno = -1;
        logger.info("NIOPS: close()- ok.");
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public InputStream getInputStream() {
        return in;
    }

    public int getPort() {
        return portno;
    }

    public int getLocalPort() {
        return localportno;
    }

    public void flush() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("flush");
        }
        // TODO: create selector for all channels which have a sendInProgress; do a select.
        for (int i = 0; i < numWays; i++) {
            if (sendInProgress[i]) {
                ChannelOutputStream o = outs[i];
                while (!o.write())
                    /* keep on writing until all is gone */;
                sendInProgress[i] = false;
            }
        }
    }

    private class ChannelOutputStream {

        SocketChannel c;

        int channelNo; // only for debug purposes

        ByteBuffer preAlloccedBuf = ByteBuffer.allocateDirect(BUF_SIZE);

        ByteBuffer sendBuf = null; // points to preAlloccedBuf or wrapped user buf

        ChannelOutputStream(SocketChannel c, int channelNo) {
            this.c = c;
            this.channelNo = channelNo;
        }

        void setBuf(int sendBlock, byte[] b, int off, int len) {
            if (logger.isDebugEnabled()) {
                logger.debug("channel write: channel = " + channelNo
                        + ", writing " + len + " bytes, off = " + off);
            }

            if (len + HEADER_SIZE < BUF_SIZE) {
                sendBuf = preAlloccedBuf;
                sendBuf.position(HEADER_SIZE);
                sendBuf.limit(len + HEADER_SIZE);
                sendBuf.put(b, off, len);
                sendBuf.putInt(0, sendBlock);
                sendBuf.putInt(4, len);
                sendBuf.position(0);
            } else {
                sendBuf = ByteBuffer.wrap(b, off, len);
                System.err.println("wrapped: pos = " + sendBuf.position()
                        + ", lim = " + sendBuf.limit() + ", cap = "
                        + sendBuf.capacity());
            }
        }

        boolean write() throws IOException {
            c.write(sendBuf);
            return sendBuf.remaining() == 0;
        }

        void close() {
            preAlloccedBuf = null;
        }
    }

    private class ChannelInputStream {
        SocketChannel c;

        int channelNo; // only for debug purposes

        byte[] tmp = new byte[1];

        ByteBuffer tmpbuf = ByteBuffer.allocateDirect(4);

        ChannelInputStream(SocketChannel c, int channelNo) {
            this.c = c;
            this.channelNo = channelNo;
        }

        void close() throws IOException {
            tmpbuf = null;
            tmp = null;
        }

        int readInt() throws IOException {
            if (logger.isDebugEnabled()) {
                logger.debug("channel readInt: channel = " + channelNo);
            }
            int bytesRead = 0;

            tmpbuf.clear();
            bytesRead = c.read(tmpbuf);

            if (bytesRead == -1)
                return -1; //throw new EOFException();

            if (bytesRead == 0)
                return -1;

            while (bytesRead != 4) {
                if (logger.isDebugEnabled()) {
                    logger.debug("AAAA, bytes read = " + bytesRead);
                    System.err.print("X");
                }
                bytesRead += c.read(tmpbuf);
            }

            int res = tmpbuf.getInt(0);
            //            System.err.println("readInt: res = " + res);
            return res;
        }

        int read(byte[] b, int off, int len) throws IOException {
            if (logger.isDebugEnabled()) {
                logger.debug("channel read: channel = " + channelNo
                        + ", reading " + len + " bytes, off = " + off);
            }
            if (len == 0)
                return 0;
            int bytesRead = 0;
            ByteBuffer buf = ByteBuffer.wrap(b, off, len);

            // @@@ TODO: select here. --Rob
            while (true) {
                int res = c.read(buf);
                if (res == -1)
                    throw new EOFException();

                bytesRead += res;
                if (bytesRead >= len)
                    break;
            }

            return bytesRead;
        }
    }
}
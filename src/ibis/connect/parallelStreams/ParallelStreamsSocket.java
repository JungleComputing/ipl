/* $Id$ */

package ibis.connect.parallelStreams;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.ConnectionProperties;
import ibis.connect.IbisSocket;
import ibis.connect.IbisSocketFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

import org.apache.log4j.Logger;

public class ParallelStreamsSocket extends IbisSocket {
    static Logger logger = Logger.getLogger(ParallelStreamsSocket.class
            .getName());

    public static final int DEFAULT_NUM_WAYS = 4;

    public static final int DEFAULT_BLOCK_SIZE = 1460;

    private PSInputStream in = null;

    private PSOutputStream out = null;

    private int portno = -1;

    private int localportno = -1;

    private static int portcounter = 1;

    private int numWays = -1;

    private int blockSize = -1;

    private Socket[] sockets = null;

    private InputStream[] ins = null;

    private OutputStream[] outs = null;

    private int sendPos = 0;

    private int sendBlock = 0;

    private int recvPos = 0;

    private int recvBlock = 0;

    private Map props;

    private boolean hint = false;

    /*
     * End-users are not supposed to call this constructor. They should use the
     * socket factory instead.
     */
    protected ParallelStreamsSocket(InputStream ctrlIs, OutputStream ctrlOs,
            boolean hint, Map p) throws IOException {
        super(p);

        String snw = (String) p.get(ConnectionProperties.PAR_NUMWAYS);
        if (snw != null) {
            numWays = Integer.parseInt(snw);
        } else {
            logger.warn("using parallel streams, but numways property not set, using default");
            numWays = DEFAULT_NUM_WAYS;
        }
        String sbs = (String) p.get(ConnectionProperties.PAR_BLOCKSIZE);
        blockSize = ParallelStreamsSocket.DEFAULT_BLOCK_SIZE;
        if (sbs != null) {
            blockSize = Integer.parseInt(sbs);
        }

        System.err.println("# ParallelStreams: building link- numWays = " + numWays
                + "; blockSize = " + blockSize);

        sockets = new Socket[numWays];
        ins = new InputStream[numWays];
        outs = new OutputStream[numWays];
        this.props = p;

        synchronized (ParallelStreamsSocket.class) {
            localportno = portcounter++;
        }
        portno = connect(ctrlIs, ctrlOs, hint, localportno);
        in = new PSInputStream();
        out = new PSOutputStream();
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

        logger.info("PS: received properties from peer.");
        if (rNumWays != numWays) {
            throw new Error("ParallelStreams: cannot connect- localNumWays = "
                    + numWays + "; remoteNumWays = " + rNumWays);
        }

        BrokeredSocketFactory f = IbisSocketFactory.getFactory()
                .getParallelStreamsBaseType();

        hint = hnt;

        logger.info("PS: connecting, numWays = " + numWays + " (hint=" + hint
                + ")");

        for (i = 0; i < numWays; i++) {
            out.flush();
            Socket s = f.createBrokeredSocket(in, out, hint, props);
            sockets[i] = s;
            ins[i] = s.getInputStream();
            outs[i] = s.getOutputStream();
        }
        return rport;
    }

    public void setReceiveBufferSize(int n) throws SocketException {
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setReceiveBufferSize(n);
            }
        }
    }

    public void setSendBufferSize(int n) throws SocketException {
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setSendBufferSize(n);
            }
        }
    }

    private int poll() throws IOException {
        int rc = 0;
        for (int i = 0; i < numWays; i++) {
            rc += ins[i].available();
            logger.info("PS: poll()- rc = " + rc);
        }
        return rc;
    }

    private int recv(byte[] b, int off, int len) throws IOException {
        logger.info("PS: recv()- off = " + off + ", len = " + len);
        int nextAvail = 0;
        int done = 0;
        do {
            int nextRead = Math.min(len, blockSize - recvPos);
            int rc = 0;
            rc = ins[recvBlock].read(b, off, nextRead);
            if (rc < 0) {
                if (done == 0) {
                    logger.info("PS: recv()- done = " + -1);
                    return -1;
                }
                break;
            }
            done += rc;
            off += rc;
            len -= rc;
            recvPos = (recvPos + rc) % blockSize;
            if (recvPos == 0) {
                recvBlock = (recvBlock + 1) % numWays;
            }
            nextAvail = ins[recvBlock].available();
            if (rc < nextRead || nextAvail == 0) {
                break;
            }
            //	} while(nextAvail > 0 && len > 0);
        } while (len > 0);
        logger.info("PS: recv()- done = " + done);
        return done;
    }

    private void send(byte[] b, int off, int len) throws IOException {
        logger.info("PS: send()- len = " + len);
        while (len > 0) {
            int l = Math.min(len, blockSize - sendPos);
            outs[sendBlock].write(b, off, l);
            outs[sendBlock].flush();
            off += l;
            len -= l;
            sendPos = (sendPos + l) % blockSize;
            if (sendPos == 0) {
                sendBlock = (sendBlock + 1) % numWays;
            }
        }
    }

    public void shutdownInput() throws IOException {
        logger.info("PS: shutdownInput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownInput();
        }
    }

    public void shutdownOutput() throws IOException {
        logger.info("PS: shutdownOutput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownOutput();
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        logger.info("PS: setTcpNoDelay");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setTcpNoDelay(on);
        }
    }

    public void setSoTimeout(int t) throws SocketException {
        logger.info("PS: setSoTimeout");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setSoTimeout(t);
        }
    }

    public String toString() {
        String result = "ParallelStreams Socket ";
        result += "(numWays = " + numWays + ")";
        return result;
    }

    public void close() throws IOException {
        logger
                .info("PS: closing PS, numWays = " + numWays + ", hint = "
                        + hint);
        for (int i = 0; i < numWays; i++) {
            ins[i].close();
            outs[i].close();
            sockets[i].close();
        }
        in = null;
        out = null;
        portno = -1;
        logger.info("PS: close()- ok.");
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

    private class PSInputStream extends InputStream {
        byte[] tmp = new byte[1];

        public PSInputStream() {
            super();
        }

        public int read(byte[] b) throws IOException {
            return this.read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return recv(b, off, len);
        }

        public int read() throws IOException {
            int rc = 0;
            while (rc == 0) {
                rc = this.read(tmp);
            }
            if (rc == -1) {
                return -1;
            }
            return tmp[0] & 255;

        }

        public int available() throws IOException {
            return poll();
        }

        public void close() {
            in = null;
        }
    }

    private class PSOutputStream extends OutputStream {
        byte[] tmp = new byte[1];

        public PSOutputStream() {
            super();
        }

        public void write(int v) throws IOException {
            tmp[0] = (byte) v;
            this.write(tmp);
        }

        public void write(byte[] b) throws IOException {
            this.write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            send(b, off, len);
        }

        public void flush() {
            // no flush needed
        }

        public void close() {
            out = null;
        }
    }
}
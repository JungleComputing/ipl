/* $Id$ */

package ibis.connect.parallelStreams;

import ibis.connect.socketFactory.BrokeredSocketFactory;
import ibis.connect.socketFactory.ConnectionPropertiesProvider;
import ibis.connect.socketFactory.ExtSocketFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Logger;

public class ParallelStreams {
    public static final int defaultNumWays = 4;

    public static final int defaultBlockSize = 1460;

    static Logger logger = ibis.util.GetLogger.getLogger(ParallelStreams.class.getName());

    private int numWays = -1;

    private int blockSize = -1;

    private Socket[] sockets = null;

    private InputStream[] ins = null;

    private OutputStream[] outs = null;

    private int sendPos = 0;

    private int sendBlock = 0;

    private int recvPos = 0;

    private int recvBlock = 0;

    private ConnectionPropertiesProvider props;

    private boolean readerBusy = false;

    private boolean writerBusy = false;

    private boolean hint = false;

    public ParallelStreams(int n, int b, ConnectionPropertiesProvider props) {
        logger.info("# ParallelStreams: building link- numWays = " + n
                + "; blockSize = " + b);
        numWays = n;
        blockSize = b;
        sockets = new Socket[n];
        ins = new InputStream[n];
        outs = new OutputStream[n];
        this.props = props;
    }

    public int connect(InputStream in, OutputStream out, boolean hnt,
            int portno) throws IOException {
        int i;

        DataOutputStream os = new DataOutputStream(out);
        os.writeInt(numWays);
        os.writeInt(portno);
        os.flush();

        DataInputStream is = new DataInputStream(in);
        int rNumWays = is.readInt();
        int rport = is.readInt();

        logger.debug("PS: received properties from peer.");
        if (rNumWays != numWays) {
            throw new Error("ParallelStreams: cannot connect- localNumWays = "
                    + numWays + "; remoteNumWays = " + rNumWays);
        }

        BrokeredSocketFactory f = ExtSocketFactory.getBrokeredType();

        hint = hnt;

        logger.debug("PS: connecting, numWays = " + numWays + " (hint=" + hint
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

    void setReceiveBufferSize(int n) throws SocketException {
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setReceiveBufferSize(n);
            }
        }
    }

    void setSendBufferSize(int n) throws SocketException {
        for (int i = 0; i < numWays; i++) {
            if (sockets[i] != null) {
                sockets[i].setSendBufferSize(n);
            }
        }
    }

    public int poll() throws IOException {
        synchronized (this) {
            while (readerBusy) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
                readerBusy = true;
            }
        }
        try {
            int rc = ins[recvBlock].available();
            logger.debug("PS: poll()- rc = " + rc);
            // AD: TODO- should be a little bit expanded to be more accurate :-)
            return rc;
        } finally {
            synchronized (this) {
                readerBusy = false;
                notifyAll();
            }
        }
    }

    public int recv(byte[] b, int off, int len) throws IOException {
        synchronized (this) {
            while (readerBusy) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
                readerBusy = true;
            }
        }
        try {
            logger.debug("PS: recv()- len = " + len);
            int nextAvail = 0;
            int done = 0;
            do {
                int nextRead = Math.min(len, blockSize - recvPos);
                int rc = ins[recvBlock].read(b, off, nextRead);
                if (rc < 0) {
                    if (done == 0) {
                        logger.debug("PS: recv()- done = " + -1);
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
            logger.debug("PS: recv()- done = " + done);
            return done;
        } finally {
            synchronized (this) {
                readerBusy = false;
                notifyAll();
            }
        }
    }

    public void send(byte[] b, int off, int len) throws IOException {
        synchronized (this) {
            while (writerBusy) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
                writerBusy = true;
            }
        }
        try {
            logger.debug("PS: send()- len = " + len);
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
        } finally {
            synchronized (this) {
                writerBusy = false;
                notifyAll();
            }
        }
    }

    public void shutdownInput() throws IOException {
        logger.debug("PS: shutdownInput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownInput();
        }
    }

    public void shutdownOutput() throws IOException {
        logger.debug("PS: shutdownOutput");
        for (int i = 0; i < numWays; i++) {
            sockets[i].shutdownOutput();
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        logger.debug("PS: setTcpNoDelay");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setTcpNoDelay(on);
        }
    }

    public void setSoTimeout(int t) throws SocketException {
        logger.debug("PS: setSoTimeout");
        for (int i = 0; i < numWays; i++) {
            sockets[i].setSoTimeout(t);
        }
    }

    public String toString() {
        return "(numWays = " + numWays + ")";
    }

    public void close() throws IOException {
        logger.debug("PS: closing PS, numWays = " + numWays + ", hint = "
                + hint);
        for (int i = 0; i < numWays; i++) {
            ins[i].close();
            outs[i].close();
            sockets[i].close();
        }
        logger.debug("PS: close()- ok.");
    }
}

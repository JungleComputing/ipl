package ibis.impl.net.def;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedInput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetReceiveBufferFactoryDefaultImpl;
import ibis.io.Conversion;

import java.io.IOException;
import java.io.InputStream;

public final class DefInput extends NetBufferedInput {

    private Integer spn = null;

    private InputStream defIs = null;

    private NetReceiveBuffer buf = null;

    static {
        System.err
                .println("WARNING: Class net.DefInput (still) uses Conversion.defaultConversion");
    }

    DefInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
        headerLength = 4;
    }

    synchronized public void setupConnection(NetConnection cnx)
            throws IOException {
        if (this.spn != null) {
            throw new Error("connection already established");
        }

        defIs = cnx.getServiceLink().getInputSubStream(this, "def");

        mtu = 1024;
        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetReceiveBufferFactoryDefaultImpl());
        } else {
            factory.setMaximumTransferUnit(mtu);
        }

        this.spn = cnx.getNum();
    }

    /* Create a NetReceiveBuffer and do a blocking receive. */
    private NetReceiveBuffer receive() throws IOException {

        NetReceiveBuffer buf = createReceiveBuffer(0);
        byte[] b = buf.data;
        int l = 0;

        int offset = 0;

        do {
            int result = defIs.read(b, offset, 4);
            if (result == -1) {
                if (offset != 0) {
                    throw new Error("broken pipe");
                }

                // System.err.println("tcp_blk: receiveByteBuffer <-- null");
                return null;
            }

            offset += result;
        } while (offset < 4);

        l = Conversion.defaultConversion.byte2int(b, 0);
        //System.err.println("received "+l+" bytes");

        do {
            int result = defIs.read(b, offset, l - offset);
            if (result == -1) {
                throw new Error("broken pipe");
            }
            offset += result;
        } while (offset < l);

        buf.length = l;

        return buf;
    }

    public Integer doPoll(boolean block) throws IOException {
        if (spn == null) {
            return null;
        }

        if (block) {
            buf = receive();
            if (buf != null) {
                return spn;
            }
        } else if (defIs.available() > 0) {
            return spn;
        }

        return null;
    }

    public void doFinish() throws IOException {
        // Avoid the buffer free by super
        // Is that correct, then?
    }

    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
            throws IOException {
        if (buf != null) {
            NetReceiveBuffer temp = buf;
            buf = null;
            return temp;
        }

        NetReceiveBuffer buf = receive();
        return buf;
    }

    public synchronized void doClose(Integer num) throws IOException {
        if (spn == num) {
            try {
                defIs.close();
            } catch (IOException e) {
                throw new Error(e);
            }

            spn = null;
        }
    }

    public void doFree() throws IOException {
        if (defIs != null) {
            defIs.close();
        }

        spn = null;
    }
}
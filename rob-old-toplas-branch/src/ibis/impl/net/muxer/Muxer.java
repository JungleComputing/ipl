/* $Id$ */

package ibis.impl.net.muxer;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.io.Conversion;
import ibis.ipl.ConnectionRefusedException;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * The UDP Multiplexer output implementation.
 *
 * <BR><B>Note</B>: this first implementation does not use UDP broadcast capabilities.
 */
public final class Muxer extends NetBufferedOutput {

    /**
     * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer rpn = null;

    private MuxerKey myKey;

    private static ibis.impl.net.NetDriver subDriver;

    private static MuxerOutput muxer;

    static {
        System.err.println("WARNING: Class net.muxer.Muxer (still)"
                + " uses Conversion.defaultConversion");
    }

    /**
     * Constructor.
     *
     * @param pt the properties of the output's 
     * {@link ibis.impl.net.NetReceivePort NetReceivePort}.
     * @param driver the TCP driver instance.
     */
    Muxer(NetPortType pt, NetDriver driver, String context) throws IOException {
        super(pt, driver, context);

        synchronized (driver) {
            if (subDriver == null) {
                // String subDriverName = getMandatoryProperty("Driver");
                // System.err.println("subDriverName = " + subDriverName);
                System.err.println("It should depend on Driver properties"
                        + " which muxer suboutput is created");
                String subDriverName = "muxer.udp";
                subDriver = driver.getIbis().getDriver(subDriverName);
                System.err.println("The subDriver is " + subDriver);
                muxer = (MuxerOutput) newSubOutput(subDriver, "muxer");
            }
        }
    }

    public void setBufferFactory(NetBufferFactory factory) {
        if (Driver.DEBUG) {
            System.err.println(this + ": +++++++++++ set a new BufferFactory "
                    + factory);
            Thread.dumpStack();
        }
        this.factory = factory;
        if (Driver.DEBUG) {
            dumpBufferFactoryInfo();
        }
    }

    /*
     * Sets up an outgoing connection over the underlying MuxerOutput.
     *
     * <BR><B>Note</B>: this function also negociate the mtu.
     * <BR><B>Note</B>: the current UDP mtu is arbitrarily fixed at 32kB.
     */
    public void setupConnection(NetConnection cnx) throws IOException {
        if (Driver.DEBUG) {
            System.err.println(this + ": setup connection, serviceLink "
                    + cnx.getServiceLink());
        }

        if (rpn != null) {
            throw new Error(Thread.currentThread() + ": " + this
                    + ": serviceLink " + cnx.getServiceLink()
                    + " -- connection already established");
        }

        rpn = cnx.getNum();

        muxer.setupConnection(cnx, this);

        headerOffset = muxer.getHeaderLength();
        headerLength = headerOffset;
        if (Driver.DEBUG) {
            headerLength += Conversion.LONG_SIZE;
        }

        mtu = muxer.getMaximumTransfertUnit();
        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetSendBufferFactoryDefaultImpl());
        } else {
            factory.setMaximumTransferUnit(mtu);
        }
        myKey = muxer.getKey(cnx);

        int ok = 0;
        ObjectInputStream is = new ObjectInputStream(
                cnx.getServiceLink().getInputSubStream(this, "muxer"));
        ok = is.readInt();
        is.close();
        if (ok != 1) {
            throw new ConnectionRefusedException("Connection handshake failed");
        }

        if (Driver.DEBUG) {
            System.err.println(this
                    + ": new output connection established, mtu " + mtu);
        }
    }

    public void sendByteBuffer(NetSendBuffer b) throws IOException {

        if (Driver.DEBUG) {
            System.err.println(this + ": try to send buffer size " + b.length);
        }
        b.connectionId = myKey;
        Conversion.defaultConversion.int2byte(myKey.remoteKey, b.data, b.base
                + Driver.KEY_OFFSET);
        if (ibis.impl.net.muxer.Driver.PACKET_SEQNO) {
            Conversion.defaultConversion.long2byte(myKey.seqno++, b.data,
                    b.base + Driver.SEQNO_OFFSET);
        }

        muxer.writeByteBuffer(b);
    }

    synchronized public void close(Integer num) throws IOException {
        if (rpn == num) {
            mtu = 0;
            rpn = null;

            muxer.disconnect(myKey);
            myKey.free();
        }
    }

    public void free() throws IOException {
        if (rpn == null) {
            return;
        }

        close(rpn);

        super.free();
    }
}

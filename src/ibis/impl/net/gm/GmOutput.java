package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

/**
 * The GM output implementation (block version).
 */
public final class GmOutput extends NetBufferedOutput {

        /**
         * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
         * local number.
         */
        private volatile Integer rpn          = null;

        private long       deviceHandle =  0;
        private long       outputHandle =  0;
        private int        lnodeId      = -1;
        private int        lportId      = -1;
        private int        lmuxId       = -1;
        private int        lockId       = -1;
        private int []     lockIds      = null;
        private int        rnodeId      = -1;
        private int        rportId      = -1;
        private int        rmuxId       = -1;
        private Driver     gmDriver     = null;


        native long nInitOutput(long deviceHandle) throws IOException;
        native int  nGetOutputNodeId(long outputHandle) throws IOException;
        native int  nGetOutputPortId(long outputHandle) throws IOException;
        native int  nGetOutputMuxId(long outputHandle) throws IOException;
        native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IOException;
        native void nSendRequest(long outputHandle, int base, int length) throws IOException;
        native void nSendBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
        native void nSendBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

        native void nSendBooleanBufferIntoRequest(long outputHandle, boolean []b, int base, int length) throws IOException;
        native void nSendBooleanBuffer(long outputHandle, boolean []b, int base, int length) throws IOException;

        native void nSendByteBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IOException;
        native void nSendByteBuffer(long outputHandle, byte []b, int base, int length) throws IOException;

         native void nSendShortBufferIntoRequest(long outputHandle, short []b, int base, int length) throws IOException;
        native void nSendShortBuffer(long outputHandle, short []b, int base, int length) throws IOException;

         native void nSendCharBufferIntoRequest(long outputHandle, char []b, int base, int length) throws IOException;
        native void nSendCharBuffer(long outputHandle, char []b, int base, int length) throws IOException;

         native void nSendIntBufferIntoRequest(long outputHandle, int []b, int base, int length) throws IOException;
        native void nSendIntBuffer(long outputHandle, int []b, int base, int length) throws IOException;

         native void nSendLongBufferIntoRequest(long outputHandle, long []b, int base, int length) throws IOException;
        native void nSendLongBuffer(long outputHandle, long []b, int base, int length) throws IOException;

         native void nSendFloatBufferIntoRequest(long outputHandle, float []b, int base, int length) throws IOException;
        native void nSendFloatBuffer(long outputHandle, float []b, int base, int length) throws IOException;

         native void nSendDoubleBufferIntoRequest(long outputHandle, double []b, int base, int length) throws IOException;
        native void nSendDoubleBuffer(long outputHandle, double []b, int base, int length) throws IOException;

        native void nCloseOutput(long outputHandle) throws IOException;

        static final int packetMTU = 4096;

        /**
         * Constructor.
         *
         * @param sp the properties of the output's
         * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
         * @param driver the GM driver instance.
         */
        GmOutput(NetPortType pt, NetDriver driver, String context)
                throws IOException {
                super(pt, driver, context);

                gmDriver = (Driver)driver;

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                outputHandle = nInitOutput(deviceHandle);
                Driver.gmAccessLock.unlock();

                arrayThreshold = 256;
        }

        /*
         * Sets up an outgoing GM connection.
         *
         * @param rpn {@inheritDoc}
         * @param is {@inheritDoc}
         * @param os {@inheritDoc}
         */
        public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }

                Driver.gmAccessLock.lock(false);
                lnodeId = nGetOutputNodeId(outputHandle);
                lportId = nGetOutputPortId(outputHandle);
                lmuxId  = nGetOutputMuxId(outputHandle);
                lockId  = lmuxId*2 + 1;

                lockIds = new int[2];
                lockIds[0] = lockId; // output lock
                lockIds[1] = 0;      // main   lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                Hashtable rInfo = null;

		ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream (this, "gm"));
		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
		os.flush();

		try {
                        rInfo = (Hashtable)is.readObject();
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

		rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
		rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
		rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();
		os.writeObject(lInfo);
		os.flush();

		Driver.gmAccessLock.lock(false);
		nConnectOutput(outputHandle, rnodeId, rportId, rmuxId);
		Driver.gmAccessLock.unlock();

		is.read();
		os.write(1);
		os.flush();

		is.close();
		os.close();

                this.rpn = cnx.getNum();

                mtu = Driver.mtu;

                log.out();
        }


        /**
         * {@inheritDoc}
         */
        public void sendByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
                if (b.length > packetMTU) {
                        /* Post the 'request' */
// System.err.print("[");
                        Driver.gmAccessLock.lock(true);
                        nSendRequest(outputHandle, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'request' send completion */
                        // gmDriver.blockingPump(lockId, lockIds);
                        gmDriver.blockingPump(lockIds);

                        /* Post the 'buffer' */
                        Driver.gmAccessLock.lock(true);
                        nSendBuffer(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'buffer' send */
                        gmDriver.blockingPump(lockId, lockIds);
// System.err.print("]");
                } else {
// System.err.print("<*");
                        Driver.gmAccessLock.lock(true);
                        nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'request' send completion */
                        // gmDriver.blockingPump(lockId, lockIds);
                        gmDriver.blockingPump(lockIds);
// System.err.print(">*");
                }

		if (! b.ownershipClaimed) {
			b.free();
		}

                log.out();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void close(Integer num) throws IOException {
                log.in();
                if (rpn == num) {
                        Driver.gmAccessLock.lock(true);
                        Driver.gmLockArray.deleteLock(lockId);

                        if (outputHandle != 0) {
                                nCloseOutput(outputHandle);
                                outputHandle = 0;
                        }

                        if (deviceHandle != 0) {
                                Driver.nCloseDevice(deviceHandle);
                                deviceHandle = 0;
                        }
                        Driver.gmAccessLock.unlock();
                        rpn = null;
                }
                log.out();
        }


        /**
         * {@inheritDoc}
         */
        public void free() throws IOException {
                log.in();
                rpn = null;

                Driver.gmAccessLock.lock(true);
                Driver.gmLockArray.deleteLock(lockId);

                if (outputHandle != 0) {
                        nCloseOutput(outputHandle);
                        outputHandle = 0;
                }

                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmAccessLock.unlock();

                super.free();
                log.out();
        }

        public void writeArray(boolean [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendBooleanBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendBooleanBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        /*
        public void writeArray(byte [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                // Post the 'request'
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                // Wait for 'request' send completion
                                gmDriver.blockingPump(lockId, lockIds);

                                // Wait for 'ack' completion
                                gmDriver.blockingPump(lockId, lockIds);

                                // Post the 'buffer'
                                Driver.gmAccessLock.lock(true);
                                nSendByteBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                // Wait for 'buffer' send
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendByteBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                // Wait for 'request' send completion
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }
        */

        public void writeArray(char [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 1;
                o <<= 1;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendCharBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendCharBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(short [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 1;
                o <<= 1;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendShortBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendShortBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(int [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 2;
                o <<= 2;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendIntBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendIntBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(long [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 3;
                o <<= 3;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendLongBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendLongBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(float [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 2;
                o <<= 2;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendFloatBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendFloatBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }

        public void writeArray(double [] b, int o, int l) throws IOException {
                flush();

                int i = 0;

                l <<= 3;
                o <<= 3;

                while (l > 0) {
                        int _l = 0;

                        if (l > packetMTU) {
                                _l = Math.min(l, mtu);

                                /* Post the 'request' */
                                Driver.gmAccessLock.lock(true);
                                nSendRequest(outputHandle, o, l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Wait for 'ack' completion */
                                gmDriver.blockingPump(lockId, lockIds);

                                /* Post the 'buffer' */
                                Driver.gmAccessLock.lock(true);
                                nSendDoubleBuffer(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'buffer' send */
                                gmDriver.blockingPump(lockId, lockIds);

                        } else {
                                _l = l;

                                Driver.gmAccessLock.lock(true);
                                nSendDoubleBufferIntoRequest(outputHandle, b, o, _l);
                                Driver.gmAccessLock.unlock();

                                /* Wait for 'request' send completion */
                                gmDriver.blockingPump(lockId, lockIds);
                        }

                        l -= _l;
                        o += _l;
                }
        }
}

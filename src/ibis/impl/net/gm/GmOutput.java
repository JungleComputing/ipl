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

        native long nInitOutput(long deviceHandle) throws NetIbisException;
        native int  nGetOutputNodeId(long outputHandle) throws NetIbisException;
        native int  nGetOutputPortId(long outputHandle) throws NetIbisException;
        native int  nGetOutputMuxId(long outputHandle) throws NetIbisException;
        native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws NetIbisException;
        native void nSendRequest(long outputHandle) throws NetIbisException;
        native void nSendBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws NetIbisException;
        native void nSendBuffer(long outputHandle, byte []b, int base, int length) throws NetIbisException;
        native void nCloseOutput(long outputHandle) throws NetIbisException;


        /**
         * Constructor.
         *
         * @param sp the properties of the output's
         * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
         * @param driver the GM driver instance.
         */
        GmOutput(NetPortType pt, NetDriver driver, String context)
                throws NetIbisException {
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
        public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
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

		try {
                        ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream (this, "gm"));
                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
                        os.flush();

                        rInfo = (Hashtable)is.readObject();
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
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

                this.rpn = cnx.getNum();

                mtu = 2*1024*1024;

                log.out();
        }


        /**
         * {@inheritDoc}
         */
        public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
                log.in();
                if (b.length > 4096) {
                        /* Post the 'request' */
                        Driver.gmAccessLock.lock(true);
                        nSendRequest(outputHandle);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'request' send completion */
                        gmDriver.blockingPump(lockId, lockIds);

                        /* Wait for 'ack' completion */
                        gmDriver.blockingPump(lockId, lockIds);

                        /* Post the 'buffer' */
                        Driver.gmAccessLock.lock(true);
                        nSendBuffer(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'buffer' send */
                        gmDriver.blockingPump(lockId, lockIds);
                } else {
                        Driver.gmAccessLock.lock(true);
                        nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock();

                        /* Wait for 'request' send completion */
                        gmDriver.blockingPump(lockId, lockIds);
                }

		if (! b.ownershipClaimed) {
			b.free();
		}

                log.out();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void close(Integer num) throws NetIbisException {
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
        public void free() throws NetIbisException {
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
}

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
        private Integer    rpn           = null;
                           
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
         * @param output the controlling output.
         */
        GmOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
                throws NetIbisException {
                super(pt, driver, up, context);
                gmDriver = (Driver)driver;

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                outputHandle = nInitOutput(deviceHandle);
                Driver.gmAccessLock.unlock(false);
        }

        /*
         * Sets up an outgoing GM connection.
         *
         * @param rpn {@inheritDoc}
         * @param is {@inheritDoc}
         * @param os {@inheritDoc}
         */
        public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }

                this.rpn = cnx.getNum();
        
                Driver.gmAccessLock.lock(false);
                lnodeId = nGetOutputNodeId(outputHandle);
                lportId = nGetOutputPortId(outputHandle);
                lmuxId  = nGetOutputMuxId(outputHandle);
                lockId  = lmuxId*2 + 1;

                lockIds = new int[2];
                lockIds[0] = lockId; // output lock
                lockIds[1] = 0;      // main   lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock(false);

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                Hashtable rInfo = null;

		try {
                        ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream ("gm"));
                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream("gm"));
                        os.flush();

                        rInfo = (Hashtable)is.readObject();
                        rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                        rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                        rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();
                        os.writeObject(lInfo);
                        os.flush();

                        Driver.gmAccessLock.lock(false);
                        nConnectOutput(outputHandle, rnodeId, rportId, rmuxId);
                        Driver.gmAccessLock.unlock(false);

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

                mtu = 2*1024*1024;
        }
        

        /**
         * {@inheritDoc}
         */
        public void sendByteBuffer(NetSendBuffer b) throws NetIbisException {
                //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length);
                
                if (b.length > 4096) {
                        /* Post the 'request' */
                        Driver.gmAccessLock.lock(true);
                        nSendRequest(outputHandle);
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'request' send completion */
                        gmDriver.pump(lockId, lockIds);

                        /* Wait for 'ack' completion */
                        gmDriver.pump(lockId, lockIds);

                        /* Post the 'buffer' */
                        Driver.gmAccessLock.lock(true);
                        nSendBuffer(outputHandle, b.data, b.base, b.length);
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'buffer' send */
                        gmDriver.pump(lockId, lockIds);
                } else {
                        Driver.gmAccessLock.lock(true);
                        //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length+" - 2");
                        nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length);
                        //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length+" - 3");
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'request' send completion */
                        gmDriver.pump(lockId, lockIds);
                }

		if (! b.ownershipClaimed) {
                        //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length+" - freeing buffer");
			b.free();
                        //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length+" - freeing buffer: done");
		}

                //System.err.println("Sending buffer, base = "+b.base+", length = "+b.length+" - ok");
        }

        public synchronized void close(Integer num) throws NetIbisException {
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
                        Driver.gmAccessLock.unlock(true);
                        rpn = null;
                }
        }
        

        /**
         * {@inheritDoc}
         */
        public void free() throws NetIbisException {
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
                Driver.gmAccessLock.unlock(true);

                super.free();
        }
}

package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

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

        native long nInitOutput(long deviceHandle) throws IbisIOException;
        native int  nGetOutputNodeId(long outputHandle) throws IbisIOException;
        native int  nGetOutputPortId(long outputHandle) throws IbisIOException;
        native int  nGetOutputMuxId(long outputHandle) throws IbisIOException;
        native void nConnectOutput(long outputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IbisIOException;
        native void nSendRequest(long outputHandle) throws IbisIOException;
        native void nSendBufferIntoRequest(long outputHandle, byte []b, int base, int length) throws IbisIOException;
        native void nSendBuffer(long outputHandle, byte []b, int base, int length) throws IbisIOException;
        native void nCloseOutput(long outputHandle) throws IbisIOException;


        /**
         * Constructor.
         *
         * @param sp the properties of the output's 
         * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
         * @param driver the GM driver instance.
         * @param output the controlling output.
         */
        GmOutput(NetPortType pt, NetDriver driver, NetIO up, String context)
                throws IbisIOException {
                super(pt, driver, up, context);

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
        public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                if (this.rpn != null) {
                        throw new Error("connection already established");
                }

                this.rpn = rpn;
        
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

                Hashtable rInfo = receiveInfoTable(is);
                rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                sendInfoTable(os, lInfo);

                Driver.gmAccessLock.lock(false);
                nConnectOutput(outputHandle, rnodeId, rportId, rmuxId);
                Driver.gmAccessLock.unlock(false);

		try {
                        is.read();
                        os.write(1);
                        os.flush();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

                mtu = 2*1024*1024;
        }

        private void pump() {
                int result = Driver.gmLockArray.lockFirst(lockIds);
                if (result == 1) {
                                /* got GM main lock, let's pump */
                        Driver.gmAccessLock.lock(false);
                        Driver.nGmThread();
                        Driver.gmAccessLock.unlock(false);
                        if (!Driver.gmLockArray.trylock(lockId)) {        
                                do {
                                (Thread.currentThread()).yield();

                                        Driver.gmAccessLock.lock(false);
                                        Driver.nGmThread();
                                        Driver.gmAccessLock.unlock(false);
                                } while (!Driver.gmLockArray.trylock(lockId));
                        }
                        
                        /* request completed, release GM main lock */
                        Driver.gmLockArray.unlock(0);

                } /* else: request already completed */
                else if (result != 0) {
                        throw new Error("invalid state");
                }
        }
        

        /**
         * {@inheritDoc}
         */
        public void sendByteBuffer(NetSendBuffer b) throws IbisIOException {

                if (b.length-b.base > 2048) {
                        /* Post the 'request' */
                        Driver.gmAccessLock.lock(true);
                        nSendRequest(outputHandle);
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'request' send completion */
                        pump();

                        /* Wait for 'ack' completion */
                        pump();

                        /* Post the 'buffer' */
                        Driver.gmAccessLock.lock(true);
                        nSendBuffer(outputHandle, b.data, b.base, b.length-b.base);
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'buffer' send */
                        pump();
                } else {
                        Driver.gmAccessLock.lock(true);
                        nSendBufferIntoRequest(outputHandle, b.data, b.base, b.length-b.base);
                        Driver.gmAccessLock.unlock(true);

                        /* Wait for 'request' send completion */
                        pump();
                }
        }

        /**
         * {@inheritDoc}
         */
        public void free() throws IbisIOException {
                rpn = null;

                Driver.gmAccessLock.lock(false);
                if (outputHandle != 0) {
                        nCloseOutput(outputHandle);
                        outputHandle = 0;
                }
                
                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmAccessLock.unlock(false);

                super.free();
        }
}

package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The GM input implementation (block version).
 */
public class GmInput extends NetBufferedInput {

	/**
	 * The connection socket.
	 */
	private ServerSocket 	      gmServerSocket = null;

	/**
	 * The communication socket.
	 */
	private Socket                gmSocket       = null;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer               rpn  	      = null;

	/**
	 * The buffer block allocator.
	 */
	private NetAllocator          allocator       = null;

        private long                  deviceHandle   =    0;
	private long                  inputHandle    =    0;
        private int                   lnodeId        =   -1;
        private int                   lportId        =   -1;
        private int                   lmuxId         =   -1;
        private int                   lockId         =   -1;
        private int []                lockIds        = null;
        private int                   rnodeId        =   -1;
        private int                   rportId        =   -1;
        private int                   rmuxId         =   -1;
        private int                   blockLen       =    0;
        private boolean               firstBlock     = true;

        static private byte [] dummyBuffer = new byte[4];
	native long nInitInput(long deviceHandle) throws IbisIOException;
        native int  nGetInputNodeId(long inputHandle) throws IbisIOException;
        native int  nGetInputPortId(long inputHandle) throws IbisIOException;
        native int  nGetInputMuxId(long inputHandle) throws IbisIOException;
        native void nConnectInput(long inputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IbisIOException;
        native void nPostBuffer(long inputHandle, byte []b, int base, int length) throws IbisIOException;
	native void nCloseInput(long inputHandle) throws IbisIOException;


	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the GM driver instance.
	 * @param input the controlling input.
	 */
	GmInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
                super(pt, driver, up, context);

                Driver.gmAccessLock.lock();
                deviceHandle = Driver.nInitDevice(0);
                inputHandle = nInitInput(deviceHandle);
                Driver.gmAccessLock.unlock();
	}


	/*
	 * Sets up an incoming GM connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
		this.rpn = rpn;
		 
                Driver.gmAccessLock.lock();
                lnodeId = nGetInputNodeId(inputHandle);
                lportId = nGetInputPortId(inputHandle);
                lmuxId  = nGetInputMuxId(inputHandle);
                lockId  = lmuxId*2 + 2;

                lockIds = new int[2];
                lockIds[0] = lockId; // input lock
                lockIds[1] = 0;      // main  lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                sendInfoTable(os, lInfo);

                Hashtable rInfo = receiveInfoTable(is);
                rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

                Driver.gmAccessLock.lock();
                nConnectInput(inputHandle, rnodeId, rportId, rmuxId);
                Driver.gmAccessLock.unlock();
                
		try {
                        os.write(1);
                        os.flush();
                        is.read();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

                mtu       = 2*1024*1024;
		allocator = new NetAllocator(mtu);
	}

        private void pump() {
                int result = Driver.gmLockArray.lockFirst(lockIds);
                if (result == 1) {
                                /* got GM main lock, let's pump */
                        Driver.gmAccessLock.lock();
                        Driver.nGmThread();
                        Driver.gmAccessLock.unlock();
                        if (!Driver.gmLockArray.trylock(lockId)) {
                                do {                       
                                        (Thread.currentThread()).yield();
                                
                                        Driver.gmAccessLock.lock();
                                        Driver.nGmThread();
                                        Driver.gmAccessLock.unlock();
                                } while (!Driver.gmLockArray.trylock(lockId));
                        }
                        
                        /* request completed, release GM main lock */
                        Driver.gmLockArray.unlock(0);

                } /* else: request already completed */
                else if (result != 0) {
                        throw new Error("invalid state");
                }
        }
        
        private boolean tryPump() {
                int result = Driver.gmLockArray.trylockFirst(lockIds);
                if (result == -1) {
                        return false;
                } else if (result == 0) {
                        return true;
                } else if (result == 1) {

                        /* got GM main lock, let's pump */
                        if (Driver.gmAccessLock.trylock()) {
                                Driver.nGmThread();
                                Driver.gmAccessLock.unlock();
                        }
                        
                        boolean value = Driver.gmLockArray.trylock(lockId);

                        /* request completed, release GM main lock */
                        Driver.gmLockArray.unlock(0);

                        return value;
                } else {
                        throw new Error("invalid state");
                }
        }
        
        /**
         * {@inheritDoc}
         */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

                if (tryPump()) {
                        activeNum  = rpn;
                        firstBlock = true;
                        initReceive();
                }

                return activeNum;
	}

	/**
	 * {@inheritDoc}
	 */
	public void receiveByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        pump();
                }
                
                Driver.gmReceiveLock.lock();

                Driver.gmAccessLock.lock();
                nPostBuffer(inputHandle, buffer.data, 0, buffer.data.length);
                Driver.gmAccessLock.unlock();

                /* Ack completion */
                pump();

                /* Communication transmission */
                pump();

                if (buffer.length == 0) {
                        buffer.length = blockLen;
                } else {
                        if (buffer.length != blockLen) {
                                System.err.println("got "+blockLen+" bytes, "+buffer.length+" bytes were required");
                                
                                throw new Error("length mismatch");
                        }
                }
                
                Driver.gmReceiveLock.unlock();
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		rpn = null;

                Driver.gmAccessLock.lock();
                if (inputHandle != 0) {
                        nCloseInput(inputHandle);
                        inputHandle = 0;
                }
                
                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmAccessLock.unlock();

		super.free();
	}
}

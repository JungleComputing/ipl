package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The GM input implementation (block version).
 */
public final class GmInput extends NetBufferedInput {

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer               spn  	      = null;

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
        private UpcallThread          upcallThread   = null;
        
        static private byte [] dummyBuffer = new byte[4];
	native long nInitInput(long deviceHandle) throws IbisIOException;
        native int  nGetInputNodeId(long inputHandle) throws IbisIOException;
        native int  nGetInputPortId(long inputHandle) throws IbisIOException;
        native int  nGetInputMuxId(long inputHandle) throws IbisIOException;
        native void nConnectInput(long inputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws IbisIOException;
        native int nPostBuffer(long inputHandle, byte []b, int base, int length) throws IbisIOException;
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

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                inputHandle = nInitInput(deviceHandle);
                Driver.gmAccessLock.unlock(false);
	}

        private final class UpcallThread extends Thread {
                private boolean end = false;

                public UpcallThread(String name) {
                        super("GmInput.UpcallThread: "+name);
                        this.setDaemon(true);
                }                

                public void run() {
                        while (!end) {
                                pump();
                                activeNum = spn;
                                firstBlock = true;
                                initReceive();
                                upcallFunc.inputUpcall(GmInput.this, activeNum);
                                activeNum = null;
                        }
                }

                public void finish() {
                        end = true;
                        this.interrupt();
                }
        }

	/*
	 * Sets up an incoming GM connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                if (this.spn != null) {
                        throw new Error("connection already established");
                }
                
		this.spn = spn;
		 
                Driver.gmAccessLock.lock(false);
                lnodeId = nGetInputNodeId(inputHandle);
                lportId = nGetInputPortId(inputHandle);
                lmuxId  = nGetInputMuxId(inputHandle);
                lockId  = lmuxId*2 + 2;

                lockIds = new int[2];
                lockIds[0] = lockId; // input lock
                lockIds[1] = 0;      // main  lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock(false);

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                sendInfoTable(os, lInfo);

                Hashtable rInfo = receiveInfoTable(is);
                rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

                Driver.gmAccessLock.lock(false);
                nConnectInput(inputHandle, rnodeId, rportId, rmuxId);
                Driver.gmAccessLock.unlock(false);
                
		try {
                        os.write(1);
                        os.flush();
                        is.read();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

                mtu       = 2*1024*1024;
		allocator = new NetAllocator(mtu);
                if (upcallFunc != null) {
                        (upcallThread = new UpcallThread(lnodeId+":"+lportId+"("+lmuxId+") --> "+rnodeId+":"+rportId+"("+rmuxId+")")).start();
                }
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
                                        // WARNING: yield 
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
        
        private boolean tryPump() {
                int result = Driver.gmLockArray.trylockFirst(lockIds);
                if (result == -1) {
                        return false;
                } else if (result == 0) {
                        return true;
                } else if (result == 1) {

                        /* got GM main lock, let's pump */
                        if (Driver.gmAccessLock.trylock(false)) {
                                Driver.nGmThread();
                                Driver.gmAccessLock.unlock(false);
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

		if (spn == null) {
			return null;
		}

                if (tryPump()) {
                        activeNum  = spn;
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

                Driver.gmAccessLock.lock(true);
                int result = nPostBuffer(inputHandle, buffer.data, 0, buffer.data.length);
                Driver.gmAccessLock.unlock(true);

                //System.err.println("receiveByteBuffer: size = "+result);

                if (result == 0) {
                        /* Ack completion */
                        pump();

                        /* Communication transmission */
                        pump();

                        if (buffer.length == 0) {
                                buffer.length = blockLen;
                        } else {
                                if (buffer.length != blockLen) {
                                        throw new Error("length mismatch: got "+blockLen+" bytes, "+buffer.length+" bytes were required");
                                }
                        }
                
                } else {
                        if (buffer.length == 0) {
                                buffer.length = result;
                        } else {
                                if (buffer.length != result) {
                                        throw new Error("length mismatch: got "+result+" bytes, "+buffer.length+" bytes were required");
                                }
                        }
                }

                Driver.gmReceiveLock.unlock();
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		spn = null;

                Driver.gmAccessLock.lock(false);
                if (inputHandle != 0) {
                        nCloseInput(inputHandle);
                        inputHandle = 0;
                }
                
                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }

                if (upcallThread != null) {
                        upcallThread.finish();
                }
                
                Driver.gmAccessLock.unlock(false);

		super.free();
	}
}

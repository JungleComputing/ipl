package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The GM input implementation (block version).
 */
public final class GmInput extends NetBufferedInput {

        private final boolean         useUpcallThreadOpt = true;


	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private volatile Integer      spn  	      = null;

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

        private Driver               gmDriver     = null;

	native long nInitInput(long deviceHandle) throws NetIbisException;
        native int  nGetInputNodeId(long inputHandle) throws NetIbisException;
        native int  nGetInputPortId(long inputHandle) throws NetIbisException;
        native int  nGetInputMuxId(long inputHandle) throws NetIbisException;
        native void nConnectInput(long inputHandle, int remoteNodeId, int remotePortId, int remoteMuxId) throws NetIbisException;
        native int nPostBuffer(long inputHandle, byte []b, int base, int length) throws NetIbisException;
	native void nCloseInput(long inputHandle) throws NetIbisException;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the GM driver instance.
	 */
	GmInput(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
                super(pt, driver, context);

                gmDriver = (Driver)driver;

                Driver.gmAccessLock.lock(false);
                deviceHandle = Driver.nInitDevice(0);
                inputHandle = nInitInput(deviceHandle);
                Driver.gmAccessLock.unlock();

                arrayThreshold = 256;
	}

	/*
	 * Sets up an incoming GM connection.
	 *
	 * @param spn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
                //System.err.println("setupConnection-->");
                if (this.spn != null) {
                        throw new Error("connection already established");
                }

                Driver.gmAccessLock.lock(false);
                lnodeId = nGetInputNodeId(inputHandle);
                lportId = nGetInputPortId(inputHandle);
                lmuxId  = nGetInputMuxId(inputHandle);
                lockId  = lmuxId*2 + 2;

                //System.err.println("initializing lockIds");
                lockIds = new int[2];
                lockIds[0] = lockId; // input lock
                lockIds[1] = 0;      // main  lock
                Driver.gmLockArray.initLock(lockId, true);
                Driver.gmAccessLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                lInfo.put("gm_mux_id", new Integer(lmuxId));
                Hashtable rInfo = null;


		try {
                        ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "gm"));
                        os.flush();

                        ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream (this, "gm"));
                        os.writeObject(lInfo);
                        os.flush();

                        rInfo = (Hashtable)is.readObject();

                        rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                        rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                        rmuxId  = ((Integer) rInfo.get("gm_mux_id") ).intValue();

                        Driver.gmAccessLock.lock(false);
                        nConnectInput(inputHandle, rnodeId, rportId, rmuxId);
                        Driver.gmAccessLock.unlock();

                        os.write(1);
                        os.flush();
                        is.read();

                        os.close();
                        is.close();
		} catch (IOException e) {
			throw new NetIbisException(e);
		} catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

                mtu = 2*1024*1024;

		allocator = new NetAllocator(mtu);
		this.spn = cnx.getNum();
                startUpcallThread();
                log.out();
	}

        /**
         * {@inheritDoc}
         */
	public Integer doPoll(boolean block) throws NetIbisException {
                log.in();
                if (spn == null) {
                        return null;
		}

                if (block) {
                        gmDriver.blockingPump(lockId, lockIds);
                } else {
                        if (!gmDriver.tryPump(lockId, lockIds)) {
                                return null;
                        }
                }

                log.out();

                return spn;
	}

        public void initReceive(Integer num) throws NetIbisException {
                firstBlock = true;
                super.initReceive(num);
        }


	/**
	 * {@inheritDoc}
	 */
	public void receiveByteBuffer(NetReceiveBuffer b) throws NetIbisException {
                log.in();

                if (firstBlock) {
                        firstBlock = false;
                } else {
                        /* Request reception */
                        gmDriver.blockingPump(lockId, lockIds);
                }

                Driver.gmReceiveLock.lock();

                Driver.gmAccessLock.lock(true);
                int result = nPostBuffer(inputHandle, b.data, 0, b.data.length);
                Driver.gmAccessLock.unlock();

                if (result == 0) {
                        /* Ack completion */
                        gmDriver.blockingPump(lockId, lockIds);

                        /* Communication transmission */
                        gmDriver.blockingPump(lockId, lockIds);

                        if (b.length == 0) {
                                b.length = blockLen;
                        } else {
                                if (b.length != blockLen) {
                                        throw new Error("length mismatch: got "+blockLen+" bytes, "+b.length+" bytes were required");
                                }
                        }

                } else {
                        if (b.length == 0) {
                                b.length = result;
                        } else {
                                if (b.length != result) {
                                        throw new Error("length mismatch: got "+result+" bytes, "+b.length+" bytes were required");
                                }
                        }
                }

                Driver.gmReceiveLock.unlock();
                log.out();
        }


        public void doFinish() throws NetIbisException {
                log.in();
                //
                log.out();
        }

        public synchronized void doClose(Integer num) throws NetIbisException {
                log.in();
                if (spn == num) {
                        Driver.gmAccessLock.lock(true);
                        Driver.gmLockArray.deleteLock(lockId);

                        if (inputHandle != 0) {
                                nCloseInput(inputHandle);
                                inputHandle = 0;
                        }

                        if (deviceHandle != 0) {
                                Driver.nCloseDevice(deviceHandle);
                                deviceHandle = 0;
                        }

                        Driver.gmAccessLock.unlock();
                        spn = null;
                }
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
                log.in();
		spn = null;

                Driver.gmAccessLock.lock(true);
                Driver.gmLockArray.deleteLock(lockId);

                if (inputHandle != 0) {
                        nCloseInput(inputHandle);
                        inputHandle = 0;
                }

                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }

                Driver.gmAccessLock.unlock();
                log.out();
	}
}

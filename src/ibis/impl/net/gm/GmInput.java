package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetMutex;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4 
import java.net.SocketTimeoutException;
*/
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

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

        private byte[]                buffer          = null;

        private long                  deviceHandle   =    0;
	private long                  inputHandle    =    0;

        private NetMutex   mutex         = new NetMutex(true);
        static private byte [] dummyBuffer = new byte[4];
	native long nInitInput(long deviceHandle) throws IbisIOException;
        native int  nGetInputNodeId(long inputHandle) throws IbisIOException;
        native int  nGetInputPortId(long inputHandle) throws IbisIOException;
        native void nConnectInput(long inputHandle, int remoteNodeId, int remotePortId) throws IbisIOException;
        native void nPrepostBuffer(long inputHandle, byte []b, int base, int length) throws IbisIOException;
	native int  nReceiveBuffer(long inputHandle, byte []b, int base, int length) throws IbisIOException;
	native void nCloseInput(long inputHandle) throws IbisIOException;


	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the GM driver instance.
	 * @param input the controlling input.
	 */
	GmInput(StaticProperties sp,
		 NetDriver        driver,
		 NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
		headerLength = 0;

                Driver.gmLock.lock();
                deviceHandle = Driver.nInitDevice(0);
                inputHandle = nInitInput(deviceHandle);
                Driver.gmLock.unlock();
	}


	/*
	 * Sets up an incoming GM connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		this.rpn = rpn;
		 
                Driver.gmLock.lock();
                int lnodeId = nGetInputNodeId(inputHandle);
                int lportId = nGetInputPortId(inputHandle);
                Driver.gmLock.unlock();

                Hashtable lInfo = new Hashtable();
                lInfo.put("gm_node_id", new Integer(lnodeId));
                lInfo.put("gm_port_id", new Integer(lportId));
                sendInfoTable(os, lInfo);

                Hashtable rInfo    = receiveInfoTable(is);
                int       rnodeId = ((Integer) rInfo.get("gm_node_id")).intValue();
                int       rportId = ((Integer) rInfo.get("gm_port_id")).intValue();
                Driver.gmLock.lock();
                //System.err.println(lnodeId+"["+lportId+"]"+" connecting from "+rnodeId+"["+rportId+"]");
                nConnectInput(inputHandle, rnodeId, rportId);
                Driver.gmLock.unlock();
                
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

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This GM polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

                Driver.gmLock.lock();
                if (buffer == null) {
                        buffer = dummyBuffer;
                        //System.err.println("preposting buffer");
                        nPrepostBuffer(inputHandle, buffer, 0, buffer.length);
                        //System.err.println("buffer preposted");
                }
                
                Driver.nGmThread();

                //System.err.println("polling");
                if (mutex.trylock()) {
                        initReceive();
                        activeNum = rpn;
                        nReceiveBuffer(inputHandle, buffer, 0, buffer.length);
                        buffer = null;
                }
                //System.err.println("polling completed");
                        Driver.gmLock.unlock();

		return activeNum;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer readByteBuffer(int expectedLength)
		throws IbisIOException {
                byte [] b = null;
                int     l =    0;
                
                //System.err.println("receiving buffer: expecting "+expectedLength+" bytes");
                Driver.gmLock.lock();
                b      = allocator.allocate();
                nPrepostBuffer(inputHandle, b, 0, b.length);

                do {
                        Driver.nGmThread();
                        Driver.gmLock.unlock();
                        Driver.gmLock.lock();
                } while (!mutex.trylock());
                
                l = nReceiveBuffer(inputHandle, b, 0, b.length);
                Driver.gmLock.unlock();
                // System.err.println("buffer received: "+l+" bytes");
                
		return new NetReceiveBuffer(b, l, allocator);
	}

	public void readByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                Driver.gmLock.lock();
                nPrepostBuffer(inputHandle, buffer.data, buffer.base, buffer.length);

                do {
                        Driver.nGmThread();
                        Driver.gmLock.unlock();
                        Driver.gmLock.lock();
                } while (!mutex.trylock());
                
                nReceiveBuffer(inputHandle, buffer.data, buffer.base, buffer.length);
                Driver.gmLock.unlock();
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		rpn = null;

                Driver.gmLock.lock();
                if (inputHandle != 0) {
                        nCloseInput(inputHandle);
                        inputHandle = 0;
                }
                
                if (deviceHandle != 0) {
                        Driver.nCloseDevice(deviceHandle);
                        deviceHandle = 0;
                }
                Driver.gmLock.unlock();

		super.free();
	}
	
}

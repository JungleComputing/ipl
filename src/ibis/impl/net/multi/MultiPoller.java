package ibis.ipl.impl.net.multi;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network input poller.
 */
public final class MultiPoller extends NetInput {

	/**
	 * The set of inputs.
	 */
	protected Vector    inputVector       = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector          = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector          = null;

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
	 */
	protected NetInput  activeInput       = null;

        protected Hashtable inputTable        = null;
        protected Hashtable headerLengthTable = null;
        protected Hashtable mtuTable          = null;
        protected Hashtable serviceTable      = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public MultiPoller(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
		inputVector       = new Vector();
		isVector          = new Vector();
		osVector          = new Vector();
                inputTable        = new Hashtable();
                headerLengthTable = new Hashtable();
                mtuTable          = new Hashtable();
                serviceTable      = new Hashtable();
	}

        private final class ServiceThread extends Thread {
                private NetServiceListener nls         =  null;
                private int                localNlsId  =    -1;
                private int                remoteNlsId =    -1;
                private Integer            spn         =  null;
                private boolean            exit        = false;
                private ObjectInputStream  is          =  null;
                private ObjectOutputStream os          =  null;

                public ServiceThread(String name, Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                        super("ServiceThread: "+name);
                        this.spn = spn;
                        this.os  = os;
                        this.is  = is;
                        this.nls = nls;

                        try {
                                remoteNlsId = is.readInt();
                                localNlsId  = nls.getId();
                                os.writeInt(localNlsId);
                                os.flush();
                        } catch (Exception e) {
                                throw new IbisIOException(e.getMessage());
                        }
                }
                
                public void run() {
                        while (!exit) {
                                try {
                                        nls.iacquire(localNlsId);
                                        int newMtu          = is.readInt();
                                        int newHeaderLength = is.readInt();
                                        nls.release();

                                        synchronized(this) {
                                                mtuTable.put(spn, new Integer(newMtu));
                                                headerLengthTable.put(spn, new Integer(newHeaderLength));
                                        }
                                        
                                        synchronized(os) {
                                                os.writeInt(remoteNlsId);
                                                os.writeInt(1);
                                                os.flush();
                                        }
                                } catch (InterruptedException e) {
                                        exit = true;
                                } catch (Exception e) {
                                        throw new Error(e.getMessage());
                                }
                        }
                }

                public void end() {
                        exit = true;
                        this.interrupt();
                }
        }
        
        public synchronized void inputUpcall(NetInput input, Integer spn) {
                activeNum = spn;
                activeInput = input;

                // Note: the MultiPoller instance is bypassed during upcall reception
                upcallFunc.inputUpcall(input, spn);

                activeInput = null;
                activeNum = null;
        }

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                try {
                        NetIbisIdentifier localId  = (NetIbisIdentifier)driver.getIbis().identifier();
                        NetIbisIdentifier remoteId = (NetIbisIdentifier)is.readObject();
                        os.writeObject(localId);

                        InetAddress localHostAddr = InetAddress.getLocalHost();
                        InetAddress remoteHostAddr = (InetAddress)is.readObject();
                        os.writeObject(localHostAddr);

                        String subContext = null;
                        if (localId.equals(remoteId)) {
                                subContext = "process";
                        } else {
                                byte [] l = localHostAddr.getAddress();
                                byte [] r = remoteHostAddr.getAddress();
                                int n = 0;
                        
                                while (n < 4 && l[n] == r[n])
                                        n++;

                                switch (n) {
                                case 4:
                                        {
                                                subContext = "node";
                                                break;
                                        }
                        
                                case 3: 
                                        {
                                                subContext = "net_c";
                                                break;
                                        }
                        
                                case 2:
                                        {
                                                subContext = "net_b";
                                                break;
                                        }
                        
                                case 1:
                                        {
                                                subContext = "net_a";
                                                break;
                                        }
                        
                                default:
                                        { 
                                                subContext = "internet";
                                                break;
                                        }
                                }
                        }
                        
                        NetInput  ni = (NetInput)inputTable.get(subContext);

                        if (ni == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                ni                      = newSubInput(subDriver, subContext);
                                inputTable.put(subContext, ni);
                                inputVector.add(ni);
                        }

                        if (upcallFunc != null) {
                                ni.setupConnection(spn, is, os, nls, this);
                        } else {
                                ni.setupConnection(spn, is, os, nls, null);
                        }

                        {
                                int mtu          = is.readInt();
                                mtuTable.put(spn, new Integer(mtu));
                                int headerLength = is.readInt();
                                headerLengthTable.put(spn, new Integer(headerLength));
                        }

                        {
                                ServiceThread st = new ServiceThread("subcontext = "+subContext+", spn = "+spn, spn, is, os, nls);
                                serviceTable.put(spn, st);
                                st.start();
                        }                        
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new IbisIOException(e);
                }
	}

	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {		
		activeInput = null;
		activeNum   = null;
		
		Iterator i = inputVector.listIterator();

	polling_loop:
		while (i.hasNext()) {
			NetInput ni  = (NetInput)i.next();
			Integer  num = null;
			
			if ((num = ni.poll()) != null) {
				activeInput  = ni;
				activeNum    = num;
                                mtu          = ((Integer)mtuTable.get(activeNum)).intValue();
                                headerOffset = ((Integer)headerLengthTable.get(activeNum)).intValue();
				break polling_loop;
			}
		}
		
		return activeNum;
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		super.finish();
		activeInput.finish();
		activeInput = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws IbisIOException {
		if (inputVector != null) {
			Iterator i = inputVector.listIterator();

			while (i.hasNext()) {
				NetInput ni = (NetInput)i.next();
				ni.free();
			}

			inputVector = null;
		}
		
                inputTable  = null;
                
                if (serviceTable != null) {
                        Enumeration e = serviceTable.keys();
                        while (e.hasMoreElements()) {
                                Object key   = e.nextElement();
                                Object value = serviceTable.remove(key);
                                ServiceThread st = (ServiceThread)value;
                                st.end();
                        }

                        serviceTable = null;
                }
                
                headerLengthTable = null;
                mtuTable          = null;
		isVector          = null;
		osVector          = null;
		activeInput       = null;

		super.free();
	}

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                return activeInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                activeInput.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws IbisIOException {
                return activeInput.readBoolean();
        }

	public byte readByte() throws IbisIOException {
                return activeInput.readByte();
        }

	public char readChar() throws IbisIOException {
                return activeInput.readChar();
        }

	public short readShort() throws IbisIOException {
                return activeInput.readShort();
        }

	public int readInt() throws IbisIOException {
                return activeInput.readInt();
        }

	public long readLong() throws IbisIOException {
                return activeInput.readLong();
        }
	
	public float readFloat() throws IbisIOException {
                return activeInput.readFloat();
        }

	public double readDouble() throws IbisIOException {
                return activeInput.readDouble();
        }

	public String readString() throws IbisIOException {
                return (String)activeInput.readString();
        }

	public Object readObject() throws IbisIOException {
                return activeInput.readObject();
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceBoolean(b, o, l);
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceByte(b, o, l);
        }

	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceChar(b, o, l);
        }

	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceShort(b, o, l);
        }

	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceInt(b, o, l);
        }

	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceLong(b, o, l);
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceFloat(b, o, l);
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                activeInput.readArraySliceObject(b, o, l);
        }

} 

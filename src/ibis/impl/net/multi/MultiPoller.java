package ibis.ipl.impl.net.multi;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * Provides a generic multiple network input poller.
 */
public final class MultiPoller extends NetInput {

        private final class Lane {
                NetConnection cnx          = null;
                NetInput      input        = null;
                int           headerLength =    0;
                int           mtu          =    0;
                ServiceThread thread       = null;
                ObjectInputStream  is      =  null;
                ObjectOutputStream os      =  null;
        }
        
        private final class ServiceThread extends Thread {
                private Lane               lane        =  null;                
                private boolean            exit        = false;

                public ServiceThread(String name, Lane lane) throws NetIbisException {
                        super("ServiceThread: "+name);
                        this.lane = lane;
                }
                
                public void run() {
                        while (!exit) {
                                try {
                                        int newMtu          = lane.is.readInt();
                                        int newHeaderLength = lane.is.readInt();

                                        synchronized(lane) {
                                                lane.mtu          = newMtu;
                                                lane.headerLength = newHeaderLength;
                                        }
                                        
                                        lane.os.writeInt(1);
                                        lane.os.flush();
                                } catch (NetIbisInterruptedException e) {
                                        exit = true;
                                } catch (NetIbisClosedException e) {
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
        

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
         * Note: This value is not set during an upcall.
	 */
	protected volatile NetInput activeInput        = null;
        protected volatile Thread   activeUpcallThread = null;

	/**
	 * The set of inputs.
	 */
        protected Hashtable inputTable        = null;
        protected Hashtable laneTable         = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public MultiPoller(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
                inputTable        = new Hashtable();
                laneTable         = new Hashtable();
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                System.err.println("GenPoller: inputUpcall-->");
                synchronized(this) {
                        while (activeInput != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }                        

                        activeInput = (NetInput)inputTable.get(spn);
                        if (activeInput == null) {
                                return;
                        }
                        activeNum = spn;
                }                

                upcallFunc.inputUpcall(input, spn);

                synchronized(this) {
                        if (activeInput == input) {
                                activeInput = null;
                                activeNum   = null;
                                notifyAll();
                        }
                }
                System.err.println("GenPoller: inputUpcall<--");                
        }

        private String getSubContext(NetIbisIdentifier localId, InetAddress localHostAddr, NetIbisIdentifier remoteId, InetAddress remoteHostAddr) {
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

                return subContext;
        }
        

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                try {
                        NetServiceLink link = cnx.getServiceLink();

                        ObjectInputStream  is = new ObjectInputStream (link.getInputSubStream ("multi"));
                        ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream("multi"));

                        NetIbisIdentifier localId  = (NetIbisIdentifier)driver.getIbis().identifier();
                        NetIbisIdentifier remoteId = (NetIbisIdentifier)is.readObject();

                        os.writeObject(localId);

                        InetAddress localHostAddr  = InetAddress.getLocalHost();
                        InetAddress remoteHostAddr = (InetAddress)is.readObject();

                        os.writeObject(localHostAddr);

                        String   subContext = getSubContext(localId, localHostAddr, remoteId, remoteHostAddr);
                        NetInput ni         = (NetInput)inputTable.get(subContext);

                        if (ni == null) {
                                String    subDriverName = getProperty(subContext, "Driver");
                                NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);
                                ni                      = newSubInput(subDriver, subContext);
                                inputTable.put(subContext, ni);
                        }

                        if (upcallFunc != null) {
                                ni.setupConnection(cnx, this);
                        } else {
                                ni.setupConnection(cnx, null);
                        }

                        Integer num  = cnx.getNum();
                        Lane    lane = new Lane();

                        lane.is           = is;
                        lane.os           = os;
                        lane.cnx          = cnx;
                        lane.input        = ni;
                        lane.mtu          = is.readInt();
                        lane.headerLength = is.readInt();
                        lane.thread       = new ServiceThread("subcontext = "+subContext+", spn = "+num, lane);

                        laneTable.put(num, lane);

                        lane.thread.start();
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
	}

	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 */
	public synchronized Integer poll() throws NetIbisException {		
		activeInput = null;
		activeNum   = null;
		
		Iterator i = inputTable.values().iterator();

	polling_loop:
		while (i.hasNext()) {
			NetInput ni  = (NetInput)i.next();
			Integer  num = null;
			
			if ((num = ni.poll()) != null) {
				activeInput  = ni;
				activeNum    = num;

                                Lane lane = null;
                                lane = (Lane)laneTable.get(num);

                                synchronized(lane) {
                                        mtu          = lane.mtu;
                                        headerOffset = lane.headerLength;
                                }
                                
				break polling_loop;
			}
		}
		
		return activeNum;
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
		super.finish();
		activeInput.finish();
                synchronized(this) {
                        activeInput = null;
                        activeNum   = null;
                        notifyAll();
                }
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (laneTable != null) {
                        Lane lane = (Lane)laneTable.get(num);

                        if (lane != null) {
                                if (lane.input != null) {
                                        lane.input.close(num);
                                }

                                if (lane.thread != null) {
                                        lane.thread.end();
                                }
                                
                                laneTable.remove(num);

                                if (activeInput == lane.input) {
                                        activeInput = null;
                                        activeNum   = null;
                                        notifyAll();
                                }
                        }
                }
                
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
                if (laneTable != null) {
                        Iterator i = laneTable.values().iterator();
                        while (i.hasNext()) {
                                Lane lane = (Lane)i.next();
                                if (lane != null && lane.thread != null) {
                                        lane.thread.end();
                                }
                                i.remove();
                        }
                }
                
		if (inputTable != null) {
                        Iterator i = inputTable.values().iterator();

                        while (i.hasNext()) {
				NetInput ni  = (NetInput)i.next();
                                if (ni != null) {
                                        ni.free();
                                }
			}
		}
                
		activeInput = null;

		super.free();
	}

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                return activeInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                activeInput.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws NetIbisException {
                return activeInput.readBoolean();
        }

	public byte readByte() throws NetIbisException {
                return activeInput.readByte();
        }

	public char readChar() throws NetIbisException {
                return activeInput.readChar();
        }

	public short readShort() throws NetIbisException {
                return activeInput.readShort();
        }

	public int readInt() throws NetIbisException {
                return activeInput.readInt();
        }

	public long readLong() throws NetIbisException {
                return activeInput.readLong();
        }
	
	public float readFloat() throws NetIbisException {
                return activeInput.readFloat();
        }

	public double readDouble() throws NetIbisException {
                return activeInput.readDouble();
        }

	public String readString() throws NetIbisException {
                return (String)activeInput.readString();
        }

	public Object readObject() throws NetIbisException {
                return activeInput.readObject();
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceBoolean(b, o, l);
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceByte(b, o, l);
        }

	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceChar(b, o, l);
        }

	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceShort(b, o, l);
        }

	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceInt(b, o, l);
        }

	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceLong(b, o, l);
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceFloat(b, o, l);
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                activeInput.readArraySliceObject(b, o, l);
        }

} 

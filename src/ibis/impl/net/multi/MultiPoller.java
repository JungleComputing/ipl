package ibis.ipl.impl.net.multi;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public class MultiPoller extends NetInput {

	/**
	 * The set of inputs.
	 */
	protected Vector    inputVector = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector    = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector    = null;

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
	 */
	protected NetInput  activeInput = null;

        protected Hashtable inputTable = null;

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
		inputVector = new Vector();
		isVector    = new Vector();
		osVector    = new Vector();
                inputTable = new Hashtable();
	}

	/**
	 * Adds a new input to the input set.
	 *
	 * @param input the input.
	 */
	private void addInput(NetInput input) {
		inputVector.add(input);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
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
                                addInput(ni);
                        }

                        ni.setupConnection(rpn, is, os, nls);
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
                                mtu          = activeInput.getMaximumTransfertUnit();
                                headerOffset = activeInput.getHeadersLength();
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
		
		isVector    = null;
		osVector    = null;
		activeInput =  null;

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

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                activeInput.readArrayBoolean(userBuffer);
        }

	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
                activeInput.readArrayByte(userBuffer);
        }

	public void readArrayChar(char [] userBuffer) throws IbisIOException {
                activeInput.readArrayChar(userBuffer);
        }

	public void readArrayShort(short [] userBuffer) throws IbisIOException {
                activeInput.readArrayShort(userBuffer);
        }

	public void readArrayInt(int [] userBuffer) throws IbisIOException {
                activeInput.readArrayInt(userBuffer);
        }

	public void readArrayLong(long [] userBuffer) throws IbisIOException {
                activeInput.readArrayLong(userBuffer);
        }

	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
                activeInput.readArrayFloat(userBuffer);
        }

	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
                activeInput.readArrayDouble(userBuffer);
        }

	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayBoolean(userBuffer, offset, length);
        }

	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayByte(userBuffer, offset, length);
        }

	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayChar(userBuffer, offset, length);
        }

	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayShort(userBuffer, offset, length);
        }

	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayInt(userBuffer, offset, length);
        }

	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayLong(userBuffer, offset, length);
        }

	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayFloat(userBuffer, offset, length);
        }

	public void readSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                activeInput.readSubArrayDouble(userBuffer, offset, length);
        }

} 

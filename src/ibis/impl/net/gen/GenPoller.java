package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network input poller.
 */
public final class GenPoller extends NetInput {

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
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
	 */
	protected NetInput  activeInput = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public GenPoller(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws IbisIOException {
		super(pt, driver, up, context);
		inputVector = new Vector();
		isVector    = new Vector();
		osVector    = new Vector();
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
	public synchronized void setupConnection(Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}
                
		NetInput ni = newSubInput(subDriver);
                if (upcallFunc != null) {
                        ni.setupConnection(spn, is, os, nls, this);
                } else {
                        ni.setupConnection(spn, is, os, nls, null);
                }
		addInput(ni);
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) {
                activeNum = spn;
                activeInput = input;

                // Note: the GenPoller instance is bypassed during upcall reception
                upcallFunc.inputUpcall(input, spn);

                activeInput = null;
                activeNum = null;
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
		subDriver   = null;
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

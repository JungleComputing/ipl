package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetReceiveBuffer;

import java.net.InetAddress;
import java.net.Socket;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network input poller.
 */
public class GenPoller extends NetInput {

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
	public GenPoller(StaticProperties staticProperties,
			 NetDriver 	  driver,
			 NetIO  	  up) {
		super(staticProperties, driver, up);
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
	 * Setup a new input.
	 *
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer               rpn,
				    ObjectInputStream     is,
				    ObjectOutputStream    os)
		throws IbisIOException {
		if (subDriver == null) {
			subDriver = driver.getIbis().getDriver(getProperty("Driver"));
		}
		
		NetInput ni = subDriver.newInput(staticProperties, this);

		ni.setupConnection(rpn, is, os);
		addInput(ni);
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
				activeInput = ni;
				activeNum   = num;
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

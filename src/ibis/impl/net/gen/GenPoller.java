package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public final class GenPoller extends NetInput {

	/**
	 * The set of inputs.
	 */

        protected Hashtable inputTable  = null;
	/**
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
	 */
	protected volatile NetInput  activeInput = null;
        protected volatile Thread    activeUpcallThread = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public GenPoller(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
                inputTable = new Hashtable();
	}

	/**
	 * Adds a new input to the input set.
	 *
	 * @param input the input.
	 */
	private void addInput(Integer num, NetInput input) {
                inputTable.put(num, input);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}
                
		NetInput ni = newSubInput(subDriver);
                if (upcallFunc != null) {
                        ni.setupConnection(cnx, this);
                } else {
                        ni.setupConnection(cnx, null);
                }
		addInput(cnx.getNum(), ni);
	}

        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                //System.err.println("GenPoller: inputUpcall--> spn = "+spn);
                synchronized(this) {
                        while (activeInput != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
                        
                        //System.err.println("GenPoller: inputUpcall - setting activeInput, spn = "+spn);
                        activeInput = (NetInput)inputTable.get(spn);
                        if (activeInput == null) {
                                //System.err.println("GenPoller: inputUpcall - setting activeInput - input closed, spn = "+spn);
                                return;
                        }
                        activeNum = spn;
                        activeUpcallThread = Thread.currentThread();
                        //System.err.println("GenPoller: inputUpcall - setting activeInput - ok, spn = "+spn);
                }
                
                //System.err.println("GenPoller: inputUpcall - upcall--> spn = "+spn);
                upcallFunc.inputUpcall(this, spn);
                //System.err.println("GenPoller: inputUpcall - upcall<-- spn = "+spn);

                synchronized(this) {
                        if (activeInput == input && activeUpcallThread == Thread.currentThread()) {
                                //System.err.println("GenPoller: inputUpcall - clearing activeInput, spn = "+spn);
                                activeInput = null;
                                activeNum   = null;
                                activeUpcallThread = null;
                                notifyAll();
                                //System.err.println("GenPoller: inputUpcall - clearing activeInput - ok, spn = "+spn);
                        }
                }
                //System.err.println("GenPoller: inputUpcall<-- spn = "+spn);
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
	public void finish() throws NetIbisException {
                //System.err.println("GenPoller: finish-->");
		super.finish();
                activeInput.finish();
                synchronized(this) {
                        activeInput = null;
                        activeNum   = null;
                        activeUpcallThread = null;
                        notifyAll();
                }
                //System.err.println("GenPoller: finish<--");
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws NetIbisException {
                //System.err.println("GenPoller: free-->");
		if (inputTable != null) {
			Iterator i = inputTable.values().iterator();

			while (i.hasNext()) {
				NetInput ni = (NetInput)i.next();
				ni.free();
                                i.remove();
			}
		}

		synchronized(this) {
                        activeInput = null;
                        activeNum   = null;
                        activeUpcallThread = null;

//                          while (activeInput != null)
//                                  wait();
                }
                
		super.free();
                //System.err.println("GenPoller: free<--");
	}

        public synchronized void close(Integer num) throws NetIbisException {
                //System.err.println("GenPoller: close-->");
		if (inputTable != null) {
                        NetInput input = (NetInput)inputTable.get(num);
                        input.close(num);
                        inputTable.remove(num);

                        if (activeInput == input) {
                                activeInput = null;
                                activeNum   = null;
                                activeUpcallThread = null;
                                notifyAll();
                        }

                }
                //System.err.println("GenPoller: close<--");
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

package ibis.ipl.impl.net;

import ibis.io.ArrayInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public abstract class NetSerializedInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	protected NetDriver                subDriver   = null;
       
	/**       
	 * The 'real' input.       
	 */       
	protected NetInput                 subInput    = null;
        private   volatile SerializationInputStream iss         = null;
	private   Hashtable                streamTable = null;
        protected volatile Thread    activeUpcallThread = null;

	public NetSerializedInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
                streamTable = new Hashtable();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
		NetInput subInput = this.subInput;

		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver);
			this.subInput = subInput;
		}
		
                if (upcallFunc != null) {
                        subInput.setupConnection(cnx, this);
                } else {
                        subInput.setupConnection(cnx, null);
                }
	}

        public abstract SerializationInputStream newSerializationInputStream() throws NetIbisException;
        

	public void initReceive() throws NetIbisException {
                byte b = subInput.readByte();

                if (b != 0) {
                        iss = newSerializationInputStream();
                        if (activeNum == null) {
                                throw new Error("invalid state: activeNum is null");
                        }
                        
                        if (iss == null) {
                                throw new Error("invalid state: stream is null");
                        }
                        
                        streamTable.put(activeNum, iss);
                } else {
                        iss = (SerializationInputStream)streamTable.get(activeNum);
                }
	}

        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                // System.err.println("NetSerializedInput: inputUpcall-->");
                synchronized(this) {
                        while (activeNum != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
                        
                        if (spn == null) {
                                throw new Error("invalid connection num");
                        }
                        
                        activeNum = spn;
                        activeUpcallThread = Thread.currentThread();
                        // System.err.println("NetSerializedInput["+this+"]: inputUpcall - activeNum = "+activeNum);
                }

                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();
                initReceive();
                upcallFunc.inputUpcall(this, spn);
                synchronized(this) {
                        if (activeNum == spn && activeUpcallThread == Thread.currentThread()) {
                                activeNum = null;
                                activeUpcallThread = null;
                                iss = null;
                                notifyAll();
                                // System.err.println("NetSerializedInput["+this+"]: inputUpcall - activeNum = "+activeNum);
                        }
                }
                        
                // System.err.println("NetSerializedInput: inputUpcall<--");
        }

	public synchronized Integer poll(boolean block) throws NetIbisException {
                if (activeNum != null) {
                        throw new Error("invalid call");
                }

                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll(block);
                if (result != null) {
                        activeNum = result;
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                        initReceive();
                }

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
		// System.err.println("SSerializationInput: finish-->");
                //iss.close();
		super.finish();
		subInput.finish();
                synchronized(this) {
                        iss = null;
                        activeNum = null;
                        activeUpcallThread = null;
                        notifyAll();
                        // System.err.println("NetSerializedInput: finish - activeNum = "+activeNum);
                }
                

		// System.err.println("SSerializationInput: finish<--");
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (subInput != null) {
                        subInput.close(num);
                }
        }
        

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		if (subInput != null) {
			subInput.free();
		}

		super.free();
	}
	

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                return subInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                subInput.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws NetIbisException {
		try {
		    return iss.readBoolean();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }
        

	public byte readByte() throws NetIbisException {
		try {
		    return iss.readByte();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }
        

	public char readChar() throws NetIbisException {
		try {
		    return iss.readChar();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public short readShort() throws NetIbisException {
		try {
		    return iss.readShort();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public int readInt() throws NetIbisException {
		try {
		    return iss.readInt();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public long readLong() throws NetIbisException {
		try {
		    return iss.readLong();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }

	
	public float readFloat() throws NetIbisException {
		try {
		    return iss.readFloat();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public double readDouble() throws NetIbisException {
		try {
		    return iss.readDouble();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public String readString() throws NetIbisException {
		try {
		    return (String)iss.readObject();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
		    throw new NetIbisException("got exception", e2);
		}
        }


	public Object readObject() throws NetIbisException {
		try {
		    return iss.readObject();
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
		    throw new NetIbisException("got exception", e2);
		}
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceBoolean(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceByte(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceChar(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceShort(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceInt(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceLong(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceFloat(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceDouble(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		}
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
		try {
		    iss.readArraySliceObject(b, o, l);
		} catch(java.io.IOException e) {
		    throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
		    throw new NetIbisException("got exception", e2);
		}
        }
}

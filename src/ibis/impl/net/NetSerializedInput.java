package ibis.ipl.impl.net;

import ibis.io.ArrayInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import java.util.Hashtable;



//2     8      16      24      32      40      48      56      64      72      80      88      96     104     112
//......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......
//      |       |       |       |       |       |       |       |       |       |       |       |       |       |

/**
 * The ID input implementation.
 */
public abstract class NetSerializedInput extends NetInput {


	/**
	 * The driver used for the 'real' input.
	 */
	protected                       NetDriver                       subDriver               = null;

	/**
	 * The 'real' input.
	 */
	protected                       NetInput                        subInput                = null;

        /**
         * The currently active {@linkplain SerializationInputStream serialization input stream}, or <code>null</code>.
         */
        private         volatile        SerializationInputStream        iss                     = null;

        /**
         * The table containing each {@linkplain SerializationInputStream serialization input stream}.
         *
         * The table is indexed by connection numbers.
         */
	private                         Hashtable                       streamTable             = null;

        /**
         * The most recently activated upcall thread if it is still alive, or <code>null</code>.
         */
        protected       volatile        Thread                          activeUpcallThread      = null;

        private         volatile        Integer                         activeNum               = null;


	public NetSerializedInput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
                streamTable = new Hashtable();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
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
                log.out();
	}

        public abstract SerializationInputStream newSerializationInputStream() throws NetIbisException;


	public void initReceive(Integer num) throws NetIbisException {
                log.in();
                activeNum = num;
                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();

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

                        if (iss == null) {
                                throw new Error("invalid state: stream not found");
                        }
                }
                log.out();
	}

        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                log.in();
                synchronized(this) {
                        if (spn == null) {
                                throw new Error("invalid connection num");
                        }

                        while (activeNum != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }

                        initReceive(spn);
                        activeUpcallThread = Thread.currentThread();
                }

                upcallFunc.inputUpcall(this, spn);
                synchronized(this) {
                        if (activeNum == spn && activeUpcallThread == Thread.currentThread()) {
                                activeNum = null;
                                activeUpcallThread = null;
                                iss = null;
                                notifyAll();
                        }
                }

                log.out();
        }

	public synchronized Integer doPoll(boolean block) throws NetIbisException {
                log.in();
                if (subInput == null) {
                        log.out();
                        return null;
                }

                Integer result = subInput.poll(block);

                log.out();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void doFinish() throws NetIbisException {
                log.in();
                //iss.close();
		subInput.finish();
                synchronized(this) {
                        iss = null;
                        activeNum = null;
                        activeUpcallThread = null;
                        notifyAll();
                        // System.err.println("NetSerializedInput: finish - activeNum = "+activeNum);
                }

                log.out();
	}

        public synchronized void doClose(Integer num) throws NetIbisException {
                log.in();
                if (subInput != null) {
                        subInput.close(num);
                }
                log.out();
        }


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
                log.in();
		if (subInput != null) {
			subInput.free();
		}
                log.out();
	}


        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
                log.out();
                return b;
        }

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                log.in();
                subInput.readByteBuffer(buffer);
                log.out();
        }

	public boolean readBoolean() throws NetIbisException {
                boolean b = false;

                log.in();
		try {
                        b = iss.readBoolean();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}

                log.out();
                return b;
        }


	public byte readByte() throws NetIbisException {
                byte b = 0;

                log.in();
		try {
                        b = iss.readByte();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return b;
        }


	public char readChar() throws NetIbisException {
                char c = 0;

                log.in();
		try {
                        c = iss.readChar();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return c;
        }


	public short readShort() throws NetIbisException {
                short s = 0;

                log.in();
		try {
                        s = iss.readShort();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return s;
        }


	public int readInt() throws NetIbisException {
                int i = 0;

                log.in();
		try {
                        i = iss.readInt();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return i;
        }


	public long readLong() throws NetIbisException {
                long l = 0;

                log.in();
		try {
                        l = iss.readLong();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return l;
        }


	public float readFloat() throws NetIbisException {
                float f = 0.0f;

                log.in();
		try {
                        f = iss.readFloat();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return f;
        }


	public double readDouble() throws NetIbisException {
                double d = 0.0;

                log.in();
		try {
                        d = iss.readDouble();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();

                return d;
        }


	public String readString() throws NetIbisException {
                String s = null;

                log.in();
		try {
                        s = (String)iss.readObject();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
                        throw new NetIbisException("got exception", e2);
		}
                log.out();

                return s;
        }


	public Object readObject() throws NetIbisException {
                Object o = null;

                log.in();
		try {
                        o = iss.readObject();
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
                        throw new NetIbisException("got exception", e2);
		}
                log.out();

                return o;
        }

	public void readArray(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(byte [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(char [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(short [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(int [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(long [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(float [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }


	public void readArray(double [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		}
                log.out();
        }

	public void readArray(Object [] b, int o, int l) throws NetIbisException {
                log.in();
		try {
                        iss.readArray(b, o, l);
		} catch(java.io.IOException e) {
                        throw new NetIbisException("got exception", e);
		} catch(ClassNotFoundException e2) {
                        throw new NetIbisException("got exception", e2);
		}
                log.out();
        }
}

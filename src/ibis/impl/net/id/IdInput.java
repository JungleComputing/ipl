package ibis.impl.net.id;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;

import java.io.IOException;


/**
 * The ID input implementation.
 */
public final class IdInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

	/**
	 * Constructor.
	 *
	 * @param pt the properties of the input's
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 */
	IdInput(NetPortType pt, NetDriver driver, String context, NetInputUpcall inputUpcall)
		throws IOException {
		super(pt, driver, context, inputUpcall);
	}

	/*
	 * Sets up an incoming ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver, upcallFunc == null ? null : this);
			this.subInput = subInput;
		}

		subInput.setupConnection(cnx);
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) throws IOException {
                // Note: the IdInput instance is bypassed during upcall reception
                upcallFunc.inputUpcall(input, spn);
        }

        public void initReceive(Integer num) {
                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();
        }

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This ID polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer doPoll(boolean block) throws IOException {
                if (subInput == null) {
                        return null;
		}

                Integer result = subInput.poll(block);

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void doFinish() throws IOException {
		subInput.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {
		if (subInput != null) {
			subInput.free();
		}
	}

        public synchronized void doClose(Integer num) throws IOException {
                if (subInput != null) {
			subInput.close(num);
			subInput = null;
		}
        }


        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IOException {
                return subInput.readByteBuffer(expectedLength);
        }

        public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
                subInput.readByteBuffer(buffer);
        }


	public boolean readBoolean() throws IOException {
                return subInput.readBoolean();
        }


	public byte readByte() throws IOException {
                return subInput.readByte();
        }


	public char readChar() throws IOException {
                return subInput.readChar();
        }


	public short readShort() throws IOException {
                return subInput.readShort();
        }


	public int readInt() throws IOException {
                return subInput.readInt();
        }


	public long readLong() throws IOException {
                return subInput.readLong();
        }


	public float readFloat() throws IOException {
                return subInput.readFloat();
        }


	public double readDouble() throws IOException {
                return subInput.readDouble();
        }


	public String readString() throws IOException {
                return (String)subInput.readString();
        }


	public Object readObject() throws IOException, ClassNotFoundException {
                return subInput.readObject();
        }


	public void readArray(boolean [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(byte [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(char [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(short [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(int [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(long [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(float [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }


	public void readArray(double [] b, int o, int l) throws IOException {
                subInput.readArray(b, o, l);
        }

	public void readArray(Object [] b, int o, int l) throws IOException, ClassNotFoundException {
                subInput.readArray(b, o, l);
        }

}

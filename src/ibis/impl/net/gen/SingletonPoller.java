package ibis.impl.net.gen;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetBufferedInputSupport;
import ibis.impl.net.NetReceiveBuffer;

import ibis.ipl.ConnectionClosedException;

import java.io.IOException;

/**
 * A specialized Poller for the case where we can derive statically from
 * the PortType Properties that there is at most one input.
 */
public class SingletonPoller extends NetPoller {


    /**
     * The driver used for the inputs.
     */
    protected NetDriver		subDriver   = null;

    /**
     * The subInput
     */
    protected NetInput		subInput;

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	this(pt, driver, context, true, inputUpcall);
    }

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param decouplePoller en/disable decoupled message delivery in this class
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   boolean decouplePoller,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	super(pt, driver, context, decouplePoller, inputUpcall);
// System.err.println(this + ": hah, I live");
    }


    /**
     * {@inheritDoc}
     */
    public boolean readBufferedSupported() {
	return subInput.readBufferedSupported();
    }


    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws IOException {
	log.in();

	if (subDriver == null) {
	    String subDriverName = getMandatoryProperty("Driver");
	    subDriver = driver.getIbis().getDriver(subDriverName);
	}

	subInput = newSubInput(subDriver, null);

	if (decouplePoller) {
	    /*
	     * If our subclass is a multiplexer, it starts all necessary
	     * upcall threads. Then we do not want an upcall thread in
	     * this class.
	     */
	} else {
// System.err.println(this + ": start upcall thread");
	    startUpcallThread();
	}

	subInput.setupConnection(cnx);

	log.out();
    }


    public Integer doPoll(boolean block) throws IOException {
	log.in();
	if (subInput == null) {
	    return null;
	}

	Integer spn = subInput.poll(block);

	log.out();

	return spn;
    }


    /**
     * {@inheritDoc}
     */
    public void doFinish() throws IOException {
	log.in();
// rcveTimer.stop();
	subInput.finish();
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void doFree() throws IOException {
	log.in();
	if (subInput != null) {
	    subInput.free();
	}
	log.out();
    }


    protected synchronized void doClose(Integer num) throws IOException {
	log.in();
	if (subInput != null) {
	    subInput.close(num);
	    subInput = null;
	}
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public int readBuffered(byte[] data, int offset, int length)
	    throws IOException {
	log.in();
	if (length < 0 || offset + length > data.length) {
	    throw new ArrayIndexOutOfBoundsException("Illegal buffer bounds");
	}
	if (! readBufferedSupported) {
	    throw new IOException("readBuffered not supported");
	}

	NetBufferedInputSupport bi = (NetBufferedInputSupport)subInput;
	int rd = bi.readBuffered(data, offset, length);
	log.out();

	return rd;
    }

    public NetReceiveBuffer readByteBuffer(int expectedLength) throws IOException {
	log.in();
	NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
	log.out();
	return b;
    }

    public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
	log.in();
	subInput.readByteBuffer(buffer);
	log.out();
    }

    public boolean readBoolean() throws IOException {
	log.in();
	boolean v = subInput.readBoolean();
	log.out();
	return v;
    }

    public byte readByte() throws IOException {
	log.in();
	byte v = subInput.readByte();
	log.out();
	return v;
    }

    public char readChar() throws IOException {
	log.in();
	char v = subInput.readChar();
	log.out();
	return v;
    }

    public short readShort() throws IOException {
	log.in();
	short v = subInput.readShort();
	log.out();
	return v;
    }

    public int readInt() throws IOException {
	log.in();
	int v = subInput.readInt();
	log.out();
	return v;
    }

    public long readLong() throws IOException {
	log.in();
	long v = subInput.readLong();
	log.out();
	return v;
    }

    public float readFloat() throws IOException {
	log.in();
	float v = subInput.readFloat();
	log.out();
	return v;
    }

    public double readDouble() throws IOException {
	log.in();
	double v = subInput.readDouble();
	log.out();
	return v;
    }

    public String readString() throws IOException {
	log.in();
	String v = (String)subInput.readString();
	log.out();
	return v;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
	log.in();
	Object v = subInput.readObject();
	log.out();
	return v;
    }

    public void readArray(boolean [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(byte [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(char [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(short [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(int [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(long [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(float [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(double [] b, int o, int l) throws IOException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

    public void readArray(Object [] b, int o, int l) throws IOException, ClassNotFoundException {
	log.in();
	subInput.readArray(b, o, l);
	log.out();
    }

}

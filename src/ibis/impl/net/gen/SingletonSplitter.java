package ibis.impl.net.gen;

import ibis.impl.net.NetBufferedOutputSupport;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSplitter;

import java.io.IOException;

/**
 * A specialized Poller for the case where we can derive statically from
 * the PortType Properties that there is at most one input.
 */
public class SingletonSplitter extends NetSplitter {

    /**
     * The driver used for the outputs.
     */
    private NetDriver subDriver = null;

    private NetOutput	subOutput;


    /**
     * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
     * @param driver the driver of this poller.
     * @param context the context.
     */
    public SingletonSplitter(NetPortType pt, NetDriver driver, String context)
	    throws IOException {
	super(pt, driver, context);
// System.err.println(this + ": hah, I live");
    }



    /**
     * {@inheritDoc}
     */
    public boolean writeBufferedSupported() {
	return subOutput.writeBufferedSupported();
    }


    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx) throws IOException {
	log.in();
	if (subDriver == null) {
	    String subDriverName = getProperty("Driver");
	    subDriver = driver.getIbis().getDriver(subDriverName);
	}

	subOutput = newSubOutput(subDriver);
	subOutput.setupConnection(cnx);

	int _mtu = subOutput.getMaximumTransfertUnit();

	if (mtu == 0  ||  mtu > _mtu) {
	    mtu = _mtu;
	}

	int _headersLength = subOutput.getHeadersLength();

	if (headerOffset < _headersLength) {
	    headerOffset = _headersLength;
	}
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void initSend() throws IOException {
	log.in();
	// super.initSend();	<< Don't believe this
// System.err.println(this + ": in initSend(); notify singleton " + singleton);
	subOutput.initSend();
	log.out();
    }

    /**
     * {@inheritDoc}
     */
    public int send() throws IOException {
	log.in();
	// super.send();		<< Don't believe this
	subOutput.send();
	log.out();

	return 0;
    }


    /**
     * {@inheritDoc}
     */
    public long finish() throws IOException {
	log.in();
	long retval = subOutput.finish();
	// super.finish();		<< Don't believe this
	log.out();

	return retval;
    }


    /*
     * {@inheritDoc}
     */
    public void free() throws IOException {
	log.in();
	if (subOutput != null) {
	    subOutput.free();
	}

	// super.free();		<< Don't believe this
	log.out();
    }


    public synchronized void close(Integer num) throws IOException {
	log.in();
	if (subOutput != null) {
	    subOutput.close(num);
	    subOutput = null;
	}
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void flushBuffer() throws IOException {
	log.in();
	if (! writeBufferedSupported) {
	    throw new IOException("writeBuffered not supported");
	}

	NetBufferedOutputSupport bo = (NetBufferedOutputSupport)subOutput;
	bo.flushBuffer();
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void writeBuffered(byte[] data, int offset, int length)
	    throws IOException {
	log.in();
	if (! writeBufferedSupported) {
	    throw new IOException("writeBuffered not supported");
	}

	NetBufferedOutputSupport bo = (NetBufferedOutputSupport)subOutput;
	bo.writeBuffered(data, offset, length);
// System.err.println(this + ": writeBuffered to singleton " + bo);
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void writeByteBuffer(NetSendBuffer buffer) throws IOException {
	log.in();
	subOutput.writeByteBuffer(buffer);
	log.out();
    }

    /**
     * Writes a boolean v to the message.
     * @param     v             The boolean v to write.
     */
    public void writeBoolean(boolean v) throws IOException {
	log.in();
	subOutput.writeBoolean(v);
	log.out();
    }

    /**
     * Writes a byte v to the message.
     * @param     v             The byte v to write.
     */
    public void writeByte(byte v) throws IOException {
	log.in();
	subOutput.writeByte(v);
	log.out();
    }

    /**
     * Writes a char v to the message.
     * @param     v             The char v to write.
     */
    public void writeChar(char v) throws IOException {
	log.in();
	subOutput.writeChar(v);
	log.out();
    }

    /**
     * Writes a short v to the message.
     * @param     v             The short v to write.
     */
    public void writeShort(short v) throws IOException {
	log.in();
	subOutput.writeShort(v);
	log.out();
    }

    /**
     * Writes a int v to the message.
     * @param     v             The int v to write.
     */
    public void writeInt(int v) throws IOException {
	log.in();
	subOutput.writeInt(v);
	log.out();
    }


    /**
     * Writes a long v to the message.
     * @param     v             The long v to write.
     */
    public void writeLong(long v) throws IOException {
	log.in();
	subOutput.writeLong(v);
	log.out();
    }

    /**
     * Writes a float v to the message.
     * @param     v             The float v to write.
     */
    public void writeFloat(float v) throws IOException {
	log.in();
	subOutput.writeFloat(v);
	log.out();
    }

    /**
     * Writes a double v to the message.
     * @param     v             The double v to write.
     */
    public void writeDouble(double v) throws IOException {
	log.in();
	subOutput.writeDouble(v);
	log.out();
    }

    /**
     * Writes a Serializable object to the message.
     * @param     v             The object v to write.
     */
    public void writeString(String v) throws IOException {
	log.in();
	subOutput.writeString(v);
	log.out();
    }

    /**
     * Writes a Serializable object to the message.
     * @param     v             The object v to write.
     */
    public void writeObject(Object v) throws IOException {
	log.in();
	subOutput.writeObject(v);
	log.out();
    }

    public void writeArray(boolean[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(byte[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(char[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(short[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(int[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(long[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(float[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(double[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

    public void writeArray(Object[] b, int o, int l) throws IOException {
	log.in();
	subOutput.writeArray(b, o, l);
	log.out();
    }

}

package ibis.ipl.impl.net.muxer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetSendBuffer;
import ibis.ipl.impl.net.NetBufferFactory;
import ibis.ipl.impl.net.NetServiceListener;

public abstract class MuxerOutput extends NetBufferedOutput {

    public abstract void disconnect(MuxerKey key)
	    throws IbisIOException;


    /**
     * Constructor. Call this from all subclass constructors.
     */
    protected MuxerOutput(NetPortType portType,
			  NetDriver   driver,
			  NetIO       up,
			  String      context) {
	super(portType, driver, up, context);
	headerLength = NetConvert.INT_SIZE;
	mtu          =    0;
    }


    public MuxerKey getKey(Integer spn) throws IbisIOException {
	MuxerKey key = locateKey(spn.intValue());
	return key;
    }


    private MuxerKeyHash keyHash = new MuxerKeyHash();

    protected void registerKey(MuxerKey q) {
	keyHash.registerKey(q);
    }

    protected MuxerKey locateKey(int n) {
	return keyHash.locateKey(n);
    }

    protected void releaseKey(MuxerKey key) throws IbisIOException {
	keyHash.releaseKey(key);
    }

}

package ibis.ipl.impl.net.muxer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetIbisException;

public abstract class MuxerOutput extends NetBufferedOutput {


    public abstract void disconnect(MuxerKey key)
	    throws NetIbisException;


    /**
     * Constructor. Call this from all subclass constructors.
     */
    protected MuxerOutput(NetPortType portType,
			  NetDriver   driver,
			  String      context) {
	super(portType, driver, context);
	headerLength = NetConvert.INT_SIZE;
	mtu          =    0;
    }


    /*
     * At connection time, we want to pass the generated MuxerKey to this.
     * The current interface does not support passing back info from the
     * subclass to this, so we must use a hashtable to record the info,
     * and get the result later by querying the hashtable.
     */
    private Hashtable	keyHash = new Hashtable();

    synchronized
    protected void registerKey(Object key, MuxerKey q)
	    throws NetIbisException {
	keyHash.put(key, q);
    }


    synchronized
    protected MuxerKey getKey(Object key) {
	return (MuxerKey)keyHash.get(key);
    }

}

package ibis.ipl.impl.net.muxer;

import java.util.Hashtable;

import java.io.IOException;

import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetPortType;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIO;
import ibis.ipl.impl.net.NetBufferedOutput;
import ibis.ipl.impl.net.NetConnection;

public abstract class MuxerOutput extends NetBufferedOutput {


    public abstract void disconnect(MuxerKey key)
	    throws IOException;


    /**
     * Constructor. Call this from all subclass constructors.
     */
    protected MuxerOutput(NetPortType portType,
			  NetDriver   driver,
			  String      context) {
	super(portType, driver, context);
	headerLength = Driver.HEADER_SIZE;
	mtu          = 0;
    }


    public void setupConnection(NetConnection cnx)
	    throws IOException {
	setupConnection(cnx, (NetIO)this);
    }

    public abstract void setupConnection(NetConnection cnx,
					 NetIO io)
	    throws IOException;

    /*
     * At connection time, we want to pass the generated MuxerKey to this.
     * The current interface does not support passing back info from the
     * subclass to this, so we must use a hashtable to record the info,
     * and get the result later by querying the hashtable.
     */
    private Hashtable	keyHash = new Hashtable();

    synchronized
    protected void registerKey(Object key, MuxerKey q)
	    throws IOException {
	keyHash.put(key, q);
    }


    synchronized
    protected MuxerKey getKey(Object key) {
	return (MuxerKey)keyHash.get(key);
    }

}

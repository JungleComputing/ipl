package ibis.connect.parallelStreams;

import java.net.Socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;

import ibis.connect.socketFactory.SocketType;
import ibis.connect.socketFactory.BrokeredSocketFactory;

public class ParallelStreamsSocketType 
    extends    SocketType
    implements BrokeredSocketFactory
{
    public ParallelStreamsSocketType() {
	super("ParallelStreams");
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
				       boolean hint,
				       ConnectProperties p)
	throws IOException
    {
	Socket s = null;
	if(p == null)
	    throw new Error("Bad property given to ParallelStreams socket factory");
	String snw = p.getProperty("NumWays");
	int numWays = ParallelStreams.defaultNumWays;
	if(snw != null) 
	    numWays = Integer.parseInt(snw);
	String sbs = p.getProperty("BlockSize");
	int blockSize = ParallelStreams.defaultBlockSize;
	if(sbs != null)
	    blockSize = Integer.parseInt(sbs);
	s = new PSSocket(numWays, blockSize, in, out, hint);
	return s; 
    }
}

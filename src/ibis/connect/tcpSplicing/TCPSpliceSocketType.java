package ibis.connect.tcpSplicing;

import java.util.Hashtable;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ibis.connect.socketFactory.SocketType;
import ibis.connect.socketFactory.ClientServerSocketFactory;
import ibis.connect.socketFactory.BrokeredSocketFactory;


// SocketType descriptor for TCP Splicing
// --------------------------------------
public class TCPSpliceSocketType extends SocketType 
    implements BrokeredSocketFactory
{
    public TCPSpliceSocketType() { super("TCPSplice"); }

    public Socket createBrokeredSocket(InputStream in, OutputStream out, boolean hint)
	throws IOException
    {
	Splice  theSplice = new Splice();
	int    splicePort = theSplice.findPort();
	String spliceHost = theSplice.getLocalHost();

	Hashtable lInfo = new Hashtable();
	lInfo.put("splice_port", new Integer(splicePort));
	lInfo.put("splice_host", spliceHost);
	ObjectOutputStream os = new ObjectOutputStream(out);
	os.writeObject(lInfo);
	os.flush();
	
	Hashtable rInfo = null;
	ObjectInputStream is = new ObjectInputStream(in);
	try {
	    rInfo = (Hashtable)is.readObject();
	} catch (ClassNotFoundException e) {
	    throw new Error(e);
	}
	int    splice_port = ((Integer) rInfo.get("splice_port")).intValue();
	String splice_host =  (String)  rInfo.get("splice_host");

	Socket s = theSplice.connectSplice(splice_host, splice_port);
	s.setTcpNoDelay(true);
	return s;
    }
}

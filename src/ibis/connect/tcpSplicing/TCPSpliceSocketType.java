package ibis.connect.tcpSplicing;

import ibis.connect.socketFactory.BrokeredSocketFactory;
import ibis.connect.socketFactory.PlainTCPSocketType;
import ibis.connect.socketFactory.SocketType;
import ibis.connect.util.MyDebug;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


// SocketType descriptor for TCP Splicing
// --------------------------------------
public class TCPSpliceSocketType extends SocketType 
    implements BrokeredSocketFactory
{
    public TCPSpliceSocketType() { super("TCPSplice"); }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
				       boolean hint,
				       ConnectProperties p)
	throws IOException
    {
	Splice  theSplice = new Splice();
	int    splicePort = theSplice.findPort();
	String spliceHost = theSplice.getLocalHost();

	DataOutputStream os = new DataOutputStream(new BufferedOutputStream(out));
	os.writeInt(splicePort);
	os.writeUTF(spliceHost);
	os.flush();
	
	DataInputStream is = new DataInputStream(new BufferedInputStream(in));
	int splice_port = is.readInt();
	String splice_host = is.readUTF();

	if (splice_host.equals(spliceHost)) {
	    // Same hostname. TcpSplice does not seem to work in that case,
	    // but surely, plain TCP should work in this case.
	    MyDebug.trace("TCPSplice requested on same node, plain Socket used");
	    theSplice.close();
	    PlainTCPSocketType tp = new PlainTCPSocketType();
	    return tp.createBrokeredSocket(in, out, hint, p);
	}

	Socket s = theSplice.connectSplice(splice_host, splice_port);
	s.setTcpNoDelay(true);
	return s;
    }
}

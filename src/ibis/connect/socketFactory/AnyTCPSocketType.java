package ibis.connect.socketFactory;

import ibis.connect.util.MyDebug;
import ibis.connect.tcpSplicing.TCPSpliceSocketType;

import ibis.util.IPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;


/**
 * Socket type that attempts to set up connections in several ways.
 * First, an ordinary client/server connection is tried. If that fails,
 * a reversed connection is tried. If that fails as well, a TCPSplice
 * connection is tried.
 */
public class AnyTCPSocketType extends SocketType 
    implements BrokeredSocketFactory, Runnable
{
    private ServerSocket server;
    private Socket accpt = null;
    boolean present = false;

    public AnyTCPSocketType() { super("AnyTCP"); }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
	    boolean hint,
	    ConnectProperties p)
	throws IOException
    {
	DataOutputStream os = new DataOutputStream(new BufferedOutputStream(out));
	DataInputStream is = new DataInputStream(new BufferedInputStream(in));

	Socket s = null;

	for (int i = 0; i < 2; i++) {
	    if (hint) {
		MyDebug.trace("AnyTCPSocketType server side attempt");
		getServerSocket();
		accpt = null;
		String host = server.getInetAddress().getCanonicalHostName();
		int port = server.getLocalPort();
		MyDebug.trace("AnyTCPSocketType server side host = " + host + ", port = " + port);
		os.writeUTF(host);
		os.writeInt(port);
		os.flush();
		int success = is.readInt();
		if (success != 0) {
		    waitForAccpt();
		}
		server.close();		// will cause exception in accept
					// when it is still running.
		if (success != 0) {
		    MyDebug.trace("AnyTCPSocketType server side succeeds");
		    tuneSocket(accpt);
		    return accpt;
		}
		MyDebug.trace("AnyTCPSocketType server side fails");
	    }
	    else {
		String host = is.readUTF();
		int port = is.readInt();
		MyDebug.trace("AnyTCPSocketType client got host = " + host + ", port = " + port);
		try {
		    s = new Socket(host, port);
		} catch(Exception e) {
		    MyDebug.trace("AnyTCPSocketType client got exception " + e);
		    e.printStackTrace();
		    os.writeInt(0);		// failure
		}
		if (s != null) {
		    os.writeInt(1);		// success!
		}
		os.flush();
		if (s != null) {
		    MyDebug.trace("AnyTCPSocketType client side attempt succeeds");
		    tuneSocket(s);
		    return s;
		}
		MyDebug.trace("AnyTCPSocketType client side attempt fails");
	    }

	    hint = ! hint;			// try the other way around
	}

	MyDebug.trace("AnyTCPSocketType TCPSplice attempt");

	TCPSpliceSocketType tp = new TCPSpliceSocketType();
	return tp.createBrokeredSocket(in, out, hint, p);
    }

    public void run() {
	synchronized(this) {
	    present = true;
	    notifyAll();
	}
	try {
	    accpt = server.accept();
	} catch(Exception e) {
	    MyDebug.trace("AnyTCPSocketType server accept got " + e);
	}
	if (accpt != null) {
	    synchronized(this) {
		notifyAll();
	    }
	}
    }

    private synchronized void waitForAccpt() {
	while (accpt == null) {
	    try {
		wait();
	    } catch(Exception e) {
	    }
	}
    }

    private void getServerSocket() throws IOException {
	server = new ServerSocket();
	server.setReceiveBufferSize(0x8000);
	server.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0), 1);
	server.setSoTimeout(60000);	// one minute
	Thread thr = new Thread(this);
	thr.start();
    }

    private static void tuneSocket(Socket s)
	throws IOException
    {
	s.setSendBufferSize(0x8000);
	s.setReceiveBufferSize(0x8000);
	s.setTcpNoDelay(true);
    }
}

package ibis.connect.socketFactory;

import ibis.connect.tcpSplicing.TCPSpliceSocketType;
import ibis.connect.util.MyDebug;
import ibis.util.IPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Socket type that attempts to set up a brokered connection in several ways.
 * First, an ordinary client/server connection is tried. If that fails,
 * a reversed connection is tried. The idea is that one end may be behind
 * a firewall and the other not. Firewalls often allow for outgoing connections,
 * so acting as a client may succeed. If that fails as well, a TCPSplice
 * connection is tried.
 */
public class AnyTCPSocketType extends SocketType 
    implements BrokeredSocketFactory
{
    private static class ServerInfo implements Runnable {
	ServerSocket server;
	private Socket accpt = null;
	boolean present = false;

	public ServerInfo() throws IOException {
	    server = new ServerSocket();
	    server.setReceiveBufferSize(0x10000);
	    server.bind(new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1);
	    server.setSoTimeout(60000);	// one minute
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

	synchronized Socket waitForAccpt() {
	    while (accpt == null) {
		try {
		    wait();
		} catch(Exception e) {
		    // ignored
		}
	    }
	    return accpt;
	}

    }

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
		ServerInfo srv = getServerSocket();
		String host = srv.server.getInetAddress().getCanonicalHostName();
		int port = srv.server.getLocalPort();
		MyDebug.trace("AnyTCPSocketType server side host = " + host + ", port = " + port);
		os.writeUTF(host);
		os.writeInt(port);
		os.flush();
		int success = is.readInt();
		if (success != 0) {
		    s = srv.waitForAccpt();
		}

		srv.server.close();	// will cause exception in accept
					// when it is still running.
		if (success != 0) {
		    MyDebug.trace("AnyTCPSocketType server side succeeds");
		    return s;
		}
		MyDebug.trace("AnyTCPSocketType server side fails");
	    }
	    else {
		String host = is.readUTF();
		int port = is.readInt();
		MyDebug.trace("AnyTCPSocketType client got host = " + host + ", port = " + port);
		InetSocketAddress target = new InetSocketAddress(host, port);
		s = new Socket();
		try {
		    s.connect(target, 2000);
		    // s.connect(target);
		    // No, a connect without timeout sometimes just hangs.
		} catch(Exception e) {
		    MyDebug.trace("AnyTCPSocketType client got exception " + e);
		    os.writeInt(0);		// failure
		    s = null;
		}
		if (s != null) {
		    os.writeInt(1);		// success!
		}
		os.flush();
		if (s != null) {
		    MyDebug.trace("AnyTCPSocketType client side attempt succeeds");
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


    private ServerInfo getServerSocket() throws IOException {
	ServerInfo s = new ServerInfo();
	Thread thr = new Thread(s);
	thr.start();
	return s;
    }
}

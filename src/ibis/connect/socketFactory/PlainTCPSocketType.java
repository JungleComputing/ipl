package ibis.connect.socketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;


// SocketType descriptor for plain TCP sockets
// -------------------------------------------
public class PlainTCPSocketType extends SocketType 
    implements ClientServerSocketFactory, BrokeredSocketFactory
{
    public PlainTCPSocketType() { super("PlainTCP"); }

    public Socket createClientSocket(InetAddress addr, int port)
	throws IOException
    {
	Socket s = new Socket(addr, port);
	tuneSocket(s);
	return s;
    }

    public ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
	throws IOException
    {
	ServerSocket s = new ServerSocket();
	s.setReceiveBufferSize(0x8000);
	s.bind(addr, backlog);
	return s;
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
				       boolean hintIsServer,
				       ConnectProperties p)
	throws IOException
    {
	Socket s = null;
	if(hintIsServer) {
	    ServerSocket server = this.createServerSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0), 1);
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    Hashtable lInfo = new Hashtable();
	    lInfo.put("tcp_address", server.getInetAddress());
	    lInfo.put("tcp_port",    new Integer(server.getLocalPort()));
	    os.writeObject(lInfo);
	    os.flush();
	    s = server.accept();
	} else {
	    ObjectInputStream is = new ObjectInputStream(in);
	    Hashtable rInfo = null;
	    try {
		rInfo = (Hashtable)is.readObject();
	    } catch (ClassNotFoundException e) {
		throw new Error(e);
	    }
	    InetAddress raddr =  (InetAddress)rInfo.get("tcp_address");
	    int         rport = ((Integer)    rInfo.get("tcp_port")   ).intValue();
	    s = this.createClientSocket(raddr, rport);
	}
	tuneSocket(s);
	return s;
    }
    private static void tuneSocket(Socket s)
	throws IOException
    {
	s.setSendBufferSize(0x8000);
	s.setReceiveBufferSize(0x8000);
	s.setTcpNoDelay(true);
    }
}

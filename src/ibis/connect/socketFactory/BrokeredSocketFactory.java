package ibis.connect.socketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

public interface BrokeredSocketFactory
{
    public Socket createBrokeredSocket(InputStream in, OutputStream out,
				       boolean hintIsServer,
				       SocketType.ConnectProperties p)
	throws IOException;
}

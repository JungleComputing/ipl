package ibis.connect.socketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public interface BrokeredSocketFactory
{
    public Socket createBrokeredSocket(InputStream in, OutputStream out,
				       boolean hintIsServer,
				       ConnectProperties p)
	throws IOException;
}

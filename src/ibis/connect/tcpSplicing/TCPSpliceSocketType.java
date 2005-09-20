/* $Id$ */

package ibis.connect.tcpSplicing;

import ibis.connect.socketFactory.BrokeredSocketFactory;
import ibis.connect.socketFactory.ConnectionPropertiesProvider;
import ibis.connect.socketFactory.PlainTCPSocketType;
import ibis.connect.socketFactory.SocketType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

// SocketType descriptor for TCP Splicing
// --------------------------------------
public class TCPSpliceSocketType extends SocketType implements
        BrokeredSocketFactory {

    static Logger logger
            = ibis.util.GetLogger.getLogger(TCPSpliceSocketType.class.getName());

    public TCPSpliceSocketType() {
        super("TCPSplice");
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hint, ConnectionPropertiesProvider p) throws IOException {
        Splice theSplice = new Splice();
        int splicePort = theSplice.findPort();
        String spliceHost = theSplice.getLocalHost();

        DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(out));
        os.writeInt(splicePort);
        os.writeUTF(spliceHost);
        os.flush();

        DataInputStream is = new DataInputStream(new BufferedInputStream(in));
        int splice_port = is.readInt();
        String splice_host = is.readUTF();

        if (splice_host.equals(spliceHost)) {
            // Same hostname. TcpSplice does not seem to work in that case,
            // but surely, plain TCP should work in this case.
            logger.debug("TCPSplice requested on same node, plain Socket used");
            theSplice.close();
            PlainTCPSocketType tp = new PlainTCPSocketType();
            return tp.createBrokeredSocket(in, out, hint, p);
        }

        Socket s = theSplice.connectSplice(splice_host, splice_port);
        s.setTcpNoDelay(true);
        return s;
    }
}

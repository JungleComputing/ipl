/* $Id$ */

package ibis.connect;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public abstract class BrokeredSocketFactory extends ClientServerSocketFactory {
    public abstract IbisSocket createBrokeredSocket(InputStream in,
            OutputStream out, boolean hintIsServer,
            Map properties) throws IOException;
}
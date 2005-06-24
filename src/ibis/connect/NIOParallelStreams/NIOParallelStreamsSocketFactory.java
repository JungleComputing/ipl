/* $Id$ */

package ibis.connect.NIOParallelStreams;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.IbisSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class NIOParallelStreamsSocketFactory extends BrokeredSocketFactory {
    public NIOParallelStreamsSocketFactory() {

    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hint, Map p) throws IOException {
        IbisSocket s = null;
        if (p == null) {
            throw new Error(
                    "Bad property given to ParallelStreams socket factory");
        }
        s = new NIOParallelStreamsSocket(in, out, hint, p);
        return s;
    }
}
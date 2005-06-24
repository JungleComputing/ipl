/* $Id$ */

package ibis.connect.parallelStreams;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.IbisSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class ParallelStreamsSocketFactory extends BrokeredSocketFactory {
    public ParallelStreamsSocketFactory() {

    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hint, Map p) throws IOException {
        IbisSocket s = null;
        if (p == null) {
            throw new Error(
                    "Bad property given to ParallelStreams socket factory");
        }
        s = new ParallelStreamsSocket(in, out, hint, p);
        return s;
    }
}
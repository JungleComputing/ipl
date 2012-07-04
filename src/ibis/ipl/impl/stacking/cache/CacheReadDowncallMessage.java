package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReadMessage;
import java.io.IOException;

class CacheReadDowncallMessage extends CacheReadMessage {

    public CacheReadDowncallMessage(ReadMessage m, CacheReceivePort port)
            throws IOException {
        super(m, port, new DowncallBufferedDataInputStream(m, port));
    }
}

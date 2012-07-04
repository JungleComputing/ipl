package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReadMessage;
import java.io.IOException;

class CacheReadUpcallMessage extends CacheReadMessage {

    public CacheReadUpcallMessage(ReadMessage m, CacheReceivePort port) 
            throws IOException {
        
        super(m, port, new UpcallBufferedDataInputStream(m, port));
    }
}

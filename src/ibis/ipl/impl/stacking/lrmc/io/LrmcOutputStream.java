package ibis.ipl.impl.stacking.lrmc.io;

import ibis.ipl.impl.stacking.lrmc.LabelRoutingMulticast;
import ibis.ipl.impl.stacking.lrmc.util.Message;
import ibis.ipl.impl.stacking.lrmc.util.MessageCache;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LrmcOutputStream extends OutputStream {

    private static final Logger logger = LoggerFactory
            .getLogger(LrmcOutputStream.class);

    private final LabelRoutingMulticast mcast;
    private final MessageCache cache;

    public int currentID = 1;
    private int currentNUM = 0;

    private boolean closed = false;
    private boolean firstPacket = false;

    private Message message;

    public LrmcOutputStream(LabelRoutingMulticast mcast, MessageCache cache) {
        this.mcast = mcast;
        this.cache = cache;
        message = cache.get();
    }

    public void reset() {
        firstPacket = true;
    }

    public void close() {
        closed = true;
    }

    public int getPrefferedBufferSize() {
        return mcast.getPrefferedMessageSize();
    }

    public byte[] getBuffer() {
        return message.buffer;
    }

    public byte[] write(int off, int len, boolean lastPacket) {

        if (closed) {
            logger.info("____ got write(" + len + ") while closed!");
            return null;
        }

        if (firstPacket) {
            firstPacket = false;
            currentNUM = 0;
        } else {
            currentNUM++;
        }

        message.off = 0;
        message.len = len;

        if (lastPacket) {
            message.num = currentNUM | Message.LAST_PACKET;
            message.id = currentID++;
        } else {
            message.num = currentNUM;
            message.id = currentID;
        }

        if (mcast.send(message)) {
            return message.buffer;
        }

        message = cache.get();
        return message.buffer;
    }

    public void write(int b) throws IOException {
        // Ouch ... fortunately, it is never used...
        write(new byte[] { (byte) (b & 0xff) }, 0, 1);
    }
}

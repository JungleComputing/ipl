package ibis.impl.net.muxer;

import java.io.IOException;

public class MuxerKey {

    int		seqno;
    int		remoteKey;


    public MuxerKey(int remoteKey) {
	this.remoteKey = remoteKey;
    }


    public void free() throws IOException {
    }

}

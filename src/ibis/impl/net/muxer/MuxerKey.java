package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetIbisException;

public class MuxerKey {

    int		seqno;
    int		remoteKey;


    public MuxerKey(int remoteKey) {
	this.remoteKey = remoteKey;
    }


    public void free() throws NetIbisException {
    }

}

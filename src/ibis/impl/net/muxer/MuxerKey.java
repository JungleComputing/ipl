package ibis.ipl.impl.net.muxer;

import ibis.ipl.impl.net.NetIbisException;

public class MuxerKey {

    private int			localKey;
    protected Integer		spn;


    public MuxerKey(Integer spn) {
	this.spn  = spn;
	this.localKey = spn.intValue();
    }

    public int localKey() {
	return localKey;
    }


    public void free() throws NetIbisException {
	spn = null;
    }

}

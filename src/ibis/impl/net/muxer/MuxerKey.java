package ibis.ipl.impl.net.muxer;

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

}

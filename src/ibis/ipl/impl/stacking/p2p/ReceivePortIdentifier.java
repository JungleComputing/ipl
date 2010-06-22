package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.impl.IbisIdentifier;

public class ReceivePortIdentifier extends ibis.ipl.impl.ReceivePortIdentifier{

	private static final long serialVersionUID = 1L;
	private int seqNum;
	
	public ReceivePortIdentifier(String name, IbisIdentifier ibis) {
		super(name, ibis);
	}

	/**
	 * @param seqNum the seqNum to set
	 */
	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	/**
	 * @return the seqNum
	 */
	public int getSeqNum() {
		return seqNum;
	}
}

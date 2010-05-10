package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.ReceivePortIdentifier;

//TODO: delete me
public class P2PRegularMessage {
	private byte[] data;
	
	public P2PRegularMessage(ReceivePortIdentifier receiver, byte[] data){
		this.setData(data);
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}
}

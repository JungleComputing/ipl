package ibis.ipl.impl.mx;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePortIdentifier;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class MxAddress implements Serializable {

	private static final long serialVersionUID = 5057056202239491976L;

	static MxAddress fromBytes(byte[] bytes) throws Exception {
		//TODO generate an address from the implementation data of an ibis identifier
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.BIG_ENDIAN);
		
		//FIXME Maybe we shouldn't use characters directly (UTF problems?)
		if(buf.getChar() != 'm' || buf.getChar() != 'x') {
			throw new Exception("Not an Mx Address");
		}
		return new MxAddress(buf.getLong(), buf.getInt());
	}
	
	final long nic_id;
	final int endpoint_id;

	protected MxAddress(String hostname, int endpoint_id) {
		this.nic_id = JavaMx.jmx_getNicId(hostname);
		this.endpoint_id = endpoint_id;
	}
	
	protected MxAddress(long nic_id, int endpoint_id) {
		this.nic_id = nic_id;
		this.endpoint_id = endpoint_id;
	}

//	@Override
	public String toString() {
		return "mx::" + Long.toHexString(nic_id) + "<" + endpoint_id + ">";
	}

	public byte[] toBytes() {
		byte[] bytes = new byte[2 * Character.SIZE + Long.SIZE + Integer.SIZE];
		
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.BIG_ENDIAN);
		//FIXME Maybe we shouldn't use characters directly (UTF problems?)
		buf.putChar('m');
		buf.putChar('x');
		buf.putLong(nic_id);
		buf.putLong(endpoint_id);
		
		return bytes;
	}
	
}

package ibis.ipl.impl.mx;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class MxAddress implements Serializable {

	private static final long serialVersionUID = 5057056202239491976L;

	static MxAddress fromBytes(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
		
		if(buf.getChar() != 'm' || buf.getChar() != 'x') {
			return null;
		}
		return new MxAddress(buf.getLong(), buf.getInt());
	}
	
	final long nicId;
	final int endpointId;

	protected MxAddress(String hostname, int endpoint_id) {
		this.nicId = JavaMx.getNicId(hostname);
		this.endpointId = endpoint_id;
	}
	
	protected MxAddress(long nicId, int endpointId) {
		this.nicId = nicId;
		this.endpointId = endpointId;
	}

	@Override
	public String toString() {
		return "mx::" + Long.toHexString(nicId) + "<" + endpointId + ">";
	}

	public byte[] toBytes() {
		byte[] bytes = new byte[2 * Character.SIZE + Long.SIZE + Integer.SIZE];
		
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
		buf.putChar('m');
		buf.putChar('x');
		buf.putLong(nicId);
		buf.putLong(endpointId);
		
		return bytes;
	}
	
}

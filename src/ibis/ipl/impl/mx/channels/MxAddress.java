package ibis.ipl.impl.mx.channels;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MxAddress implements Serializable {

	private static final long serialVersionUID = 366681380585852309L;
	protected static final int SIZE = (2 * Character.SIZE + Long.SIZE + Integer.SIZE) / 8;
	
	final long nicId;
	final int endpointId;

	public static MxAddress fromBytes(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
		return fromByteBuffer(buf);
	}

	public static MxAddress fromByteBuffer(ByteBuffer buf) {
		if(buf.getChar() != 'm' || buf.getChar() != 'x') {
			return null;
		}
		return new MxAddress(buf.getLong(), buf.getInt());
	}
	
	public MxAddress(String hostname, int endpoint_id) {
		this.nicId = JavaMx.getNicId(hostname);
		this.endpointId = endpoint_id;
	}
	
	public MxAddress(long nicId, int endpointId) {
		this.nicId = nicId;
		this.endpointId = endpointId;
	}

	@Override
	public String toString() {
		return "mx::" + Long.toHexString(nicId) + "<" + endpointId + ">";
	}

	public byte[] toBytes() {
		byte[] bytes = new byte[SIZE];
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
		buf.putChar('m');
		buf.putChar('x');
		buf.putLong(nicId);
		buf.putInt(endpointId);
		return bytes;
	}

}

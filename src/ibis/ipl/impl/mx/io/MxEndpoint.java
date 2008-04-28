package ibis.ipl.impl.mx.io;

import java.nio.ByteBuffer;

public class MxEndpoint {
	
	MxAddress address;
	private int connectionId;

	public MxEndpoint() {
		this.connectionId = JavaMx.jmx_openEndpoint();
		this.address = new MxAddress(JavaMx.jmx_getMyNicId(connectionId), JavaMx.jmx_getMyEndpointId(connectionId));
	}
	
	protected MxAddress getAddress(String hostname, int endpoint_id) {
		return new MxAddress(hostname, endpoint_id);
	}
	
	public MxAddress getMyAddress() {
		return address;
	}
	
	public boolean connect() {
		
	}
	
	public boolean close() {
		return JavaMx.jmx_closeEndpoint();
	}
	
	public void finalize() {
		JavaMx.jmx_finalize();
	}
	
/* interface benodigd voor send/receiveports */
	
	public MxConnection Listen() {
		return null;	
	}
	
	public int Receive(ByteBuffer buf, MxAddress source) {
		return 0;
	}
	
	public MxWritableChannel Connect(MxAddress dest) {
		return null;
	}
	
	public int Send(ByteBuffer buf, MxAddress dest) {
		return 0;
	}
	
	public boolean Close(MxConnection conn) {
		return false;
	}
	
}

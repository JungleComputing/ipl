/**
 * 
 */
package ibis.ipl.impl.mx.io;

import java.nio.ByteBuffer;


/**
 * @author Timo van Kessel
 *
 */
public class NioMx {
	public static final long MATCH_DATA = 64;
	public static final long MATCH_CONNECT = 65;
	
	protected static native boolean jmx_init() throws MXException;
	
	private static native long jmx_getMyNicID();
	private static native int jmx_getMyEndpointId();
	protected static native long jmx_getNicID(String name);
	
	protected static native int jmx_connect(long nic_id, int endpoint_id);
	protected static native boolean jmx_disconnect(int connection_id);
	protected static native boolean jmx_finalize();
	
	protected static native boolean jmx_send(ByteBuffer buffer, int target, long match_send); // returns #bytes sent
	protected static native boolean jmx_send(byte[] data, int target, long match_send); // returns #bytes sent
	protected static native int jmx_asend(ByteBuffer buffer, int target, long match_send); // returns a handle
	
	/*
	protected static native boolean jmx_ssend(ByteBuffer buffer, int target, long match_send); // returns #bytes sent
	protected static native boolean jmx_ssend(byte[] data, int target, long match_send); // returns #bytes sent
	protected static native int jmx_assend(ByteBuffer buffer, int target, long match_send); // returns a handle
	*/
	
	protected static native int jmx_recv(ByteBuffer buffer, long match_recv); //returns #bytes received
	protected static native byte[] jmx_recv(long match_recv); //returns #bytes received
	protected static native int jmx_arecv(ByteBuffer buffer, long match_recv); // returns a handle
	
	protected static native boolean jmx_wait(int handle);
	protected static native boolean jmx_test(int handle);
	
	/*
	protected static native boolean jmx_probe();
	 */		
	static {
		System.loadLibrary("myriexpress");
		System.loadLibrary("javamx");
//		System.load("/home0/tpkessel/javatests/myribench/javamx/libjavamx.so");
		try {
			if(jmx_init() == false) {
				System.err.println("Error initializing JavaMX library:");
				System.exit(1);
			}
		} catch (MXException e) {
			// TODO Auto-generated catch block		
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected static MxAddress getAddress(String hostname, int endpoint_id) {
		return new MxAddress(hostname, endpoint_id);
	}
	
	protected static MxAddress getMyAddress() {
		return new MxAddress(jmx_getMyNicID(), jmx_getMyEndpointId());
	}
	
	public static boolean close() {
		return jmx_finalize();
	}
}

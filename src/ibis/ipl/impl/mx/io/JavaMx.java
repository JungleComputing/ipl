package ibis.ipl.impl.mx.io;

import java.nio.ByteBuffer;

class JavaMx implements Matching {
	
	static int endpointsInUse = 0;
	static boolean initialized = false;
	
	static {
		System.loadLibrary("myriexpress");
		System.loadLibrary("javamx");
		initialized = jmx_init();
		if(!initialized) {
			System.err.println("Error initializing JavaMX library:");
			System.exit(1);
		}
	}
	
	protected static native boolean jmx_init();
	protected static native boolean jmx_finalize();
	
	protected static native int jmx_openEndpoint();
	protected static native boolean jmx_closeEndpoint(int connectionId);
	
	protected static native long jmx_getMyNicId();
	protected static native int jmx_getMyEndpointId();
	protected static native long jmx_getNicId(String name);
	
	protected static native int jmx_connect(long nic_id, int endpoint_id);
	protected static native boolean jmx_disconnect(int connection_id);

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
	
}

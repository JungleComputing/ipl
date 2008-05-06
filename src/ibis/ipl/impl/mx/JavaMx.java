package ibis.ipl.impl.mx;

import java.nio.ByteBuffer;

final class JavaMx {
		
	static final class HandleManager {
		int size, blockSize, maxSize, activeHandles;
		boolean[] inUse;
		
		private HandleManager(int blockSize, int maxSize) {
			this.size = this.blockSize = blockSize;
			this.maxSize = maxSize;
			activeHandles = 0;
			inUse = new boolean[size];
			
			// init calls to C library
		}
		
		private native boolean addHandles(int number);
		
		protected synchronized int getHandle() {
			if(activeHandles == size) {
				//Size up handle array, also in C code
				addHandles(blockSize);
			}
			
			int handle = 0;
			while(inUse[handle] ) {
				handle++;
			}
			inUse[handle] = true;
			activeHandles++;
			return handle;
		}
		
		protected synchronized void releaseHandle(int handle) {
			inUse[handle] = false;
			activeHandles--;
		}
		
	}
	
	static final class LinkManager {
		int size, blockSize, maxSize, activeTargets;
		boolean[] inUse;
		
		private native boolean addLinks(int number);
		
		private LinkManager(int blockSize, int maxSize) {
			this.size = this.blockSize = blockSize;
			this.maxSize = maxSize;
			activeTargets = 0;
			inUse = new boolean[size];
			
			// init calls to C library
		}
		
		protected synchronized int getLink() {
			if(activeTargets == size) {
				//Size up target array, also in C code
				addLinks(blockSize);
			}
			
			int target = 0;
			while(inUse[target] ) {
				target++;
			}
			inUse[target] = true;
			activeTargets++;
			return target;
		}
		
		protected synchronized void releaseLink(int target) {
			inUse[target] = false;
			activeTargets--;
		}
		
		
	}
	
	static int endpointsInUse = 0;
	static boolean initialized = false;
	static HandleManager handles;
	static LinkManager links;
	
	static {
		System.loadLibrary("myriexpress");
		System.loadLibrary("javamx");
		initialized = jmx_init();
		if(!initialized) {
			System.err.println("Error initializing JavaMX library.");
			System.exit(1);
		}
		handles = new HandleManager(128, 128*1024); //TODO come up with better values?
		links = new LinkManager(128, 128*1024); //TODO come up with better values?
	}
	
	static native boolean jmx_init();
	static native boolean jmx_finalize();
	
	static native int jmx_NewHandler(); //TODO adapt c library, old "openEndpoint"
	static native boolean jmx_closeHandler(int endpointId); //TODO adapt c library
	
	static native long jmx_getMyNicId(int endpointId); //TODO adapt c library
	static native int jmx_getMyEndpointId(int endpointId); //TODO adapt c library
	static native long jmx_getNicId(String name);
	
	static native boolean jmx_connect(long nic_id, int endpoint_id, int link);
	static native boolean jmx_disconnect(int link);

	//TODO: implement bufsize in c library
	static native void jmx_send(ByteBuffer buffer, int bufsize, int target, long match_send, int handle); // returns a handle
	static native void jmx_sendSynchronous(ByteBuffer buffer, int bufsize, int target, long match_send, int handle); // returns a handle
	
	//TODO: implement bufsize in c library
	static native void jmx_recv(ByteBuffer buffer, int bufsize, long matchRecv, int handle); // returns a handle
	static native void jmx_recv(ByteBuffer buffer, int bufsize, long matchRecv, long matchMask, int handle); // returns a handle
	
	// TODO adapt return value in c library
	static native int jmx_wait(int handle); // return the numbers of bytes written or read
	static native int jmx_test(int handle); // return the numbers of bytes written or read, -1 if not finished yet
	
	/*
	static native boolean jmx_probe();
	*/		
		
}

package ibis.ipl.impl.mx;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

final class JavaMx {
	
	static final class HandleManager {
		private static Logger logger = Logger.getLogger(HandleManager.class);
		
		int size, blockSize, maxBlocks, activeHandles;
		boolean[] inUse;
		
		/**
		 * @param blockSize The size of the memory blocks used
		 * @param maxBlocks The maximum number of blocks
		 * @throws MxException
		 */
		private HandleManager(int blockSize, int maxBlocks) throws MxException {			
			this.size = size;
			this.blockSize = blockSize;
			this.maxBlocks = maxBlocks;
			this.activeHandles = 0;
			this.inUse = new boolean[size];
			if (init(blockSize, maxBlocks) == false) {
				throw new MxException("HandleManager: could not initialize Handles.");
			}
			// init calls to C library
		}
		
		private native boolean init(int blockSize, int maxBlocks);
		private native boolean addBlock();
		
		/**
		 * Allocates a request handle in native code
		 * @return the handle identifier
		 */
		protected synchronized int getHandle() {
			if(activeHandles == size) {
				//Size up handle array, also in C code
				if(addBlock() == false) {
					//TODO exception
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Block added");
				}
				size += blockSize;
				boolean[] temp = new boolean[size];
				for(int i = 0; i < inUse.length; i++) {
					temp[i] = inUse[i];
				}
				inUse = temp;
			}
			
			int handle = 0;
			while(inUse[handle] ) {
				// TODO bound check
				handle++;
			}
			inUse[handle] = true;
			activeHandles++;
			if (logger.isDebugEnabled()) {
				logger.debug("Handle " + handle + " distributed");
			}
			return handle;
		}
		
		/**
		 * release a handle that is not in use anymore
		 * @param handle the handle 
		 */
		protected synchronized void releaseHandle(int handle) {	
			inUse[handle] = false;
			activeHandles--;
			if (logger.isDebugEnabled()) {
				logger.debug("Handle " + handle + " freed");
			}
		}
		
	}
	
	static final class LinkManager {
		int size, blockSize, maxBlocks, activeTargets;
		boolean[] inUse;
		
		/**
		 * @param blockSize The size of the memory blocks used
		 * @param maxBlocks The maximum number of blocks
		 * @throws MxException
		 */
		private LinkManager(int blockSize, int maxBlocks) throws MxException {
			this.size = 0;
			this.blockSize = blockSize;
			this.maxBlocks = maxBlocks;
			this.activeTargets = 0;
			inUse = new boolean[size];
			if (init(blockSize, maxBlocks) == false) {
				throw new MxException("LinkManager: could not initialize Links.");
			}
			// init calls to C library
		}
		
		private native boolean init(int blockSize, int maxBlocks);
		private native boolean addBlock();
		
		/**
		 * Allocates a link data structure in native code
		 * @return the link identifier
		 */
		protected synchronized int getLink() {
			if(activeTargets == size) {
				//Size up target array, also in C code
				if(addBlock() == false) {
					//TODO exception
				}
				size += blockSize;
				boolean[] temp = new boolean[size];
				for(int i = 0; i < inUse.length; i++) {
					temp[i] = inUse[i];
				}
				inUse = temp;
			}
			
			int target = 0;
			while(inUse[target] ) {
				target++;
			}
			inUse[target] = true;
			activeTargets++;
			return target;
		}
		
		/**
		 * releases a link
		 * @param the link that can be released
		 */
		protected synchronized void releaseLink(int target) {
			inUse[target] = false;
			activeTargets--;
		}
	}
	
	private static Logger logger = Logger.getLogger(JavaMx.class);
	
	static boolean initialized = false;
	static HandleManager handles;
	static LinkManager links;
	
	static {
		try {
			System.loadLibrary("myriexpress");
			System.loadLibrary("javamx");
			
			initialized = init();
			if(!initialized) {
				if (logger.isDebugEnabled()) {
					logger.debug("Initializing JavaMX library failed.");
				}
			} else {
				//TODO choose nice values here
				handles = new HandleManager(128, 128*1024);
				links = new LinkManager(128, 128*1024);
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("error initializing JavaMx: " + e.getMessage());
			}
			initialized = false;
		}	
	}
	
	/**
	 * Initializes the JavaMx library.
	 * @return True when successful.
	 */
	static native boolean init();
	
	/**
	 * Stops the library.
	 * @return always true?
	 */
	static native boolean deInit();
	
	/**
	 * Opens a new endpoint.
	 * @param filter The filter that is used for this endpoint.
	 * @return The endpoint identifier.
	 */
	static native int newEndpoint(int filter); 
	
	/**
	 * Closes an endpoint.
	 * @param endpointId the endpoint identifier of the endpoint that will be closed.
	 */
	static native void closeEndpoint(int endpointId);
	
	/**
	 * @param endpointId The endpoint.
	 * @return The NIC ID of the NIC the endpoint located at.
	 */
	static native long getMyNicId(int endpointId);
	
	/**
	 * @param endpointId The JavaMx endpoint ID.
	 * @return The endpoint identifier that is used in the mx library. This can be different than the identifier used in JavaMx. 
	 */
	static native int getMyEndpointId(int endpointId);
	
	/**
	 * Gets the NIC identifier of a host by its name.
	 * @param name The name to resolve.
	 * @return The NIC identifier.
	 */
	static native long getNicId(String name);
	
	/**
	 * Sets up a connection for writing to a remote endpoint. Connections are one-way in JavaMx. 
	 * @param endpointId The local endpoint.
	 * @param link The identifier of the link data structure which can be used for this connection.
	 * @param targetNicId The NIC ID of the receiver side.
	 * @param targetEndpoint The (native) endpoint id of the receiver side.
	 * @param filter The filter which should be used for this connection.
	 * @return True when successful, false if something went wrong.
	 */
	static native boolean connect(int endpointId, int link, long targetNicId, int targetEndpoint, int filter);
	
	/**
	 * Sets up a connection for writing to a remote endpoint. Connections are one-way in JavaMx. 
	 * @param endpointId The local endpoint.
	 * @param link The identifier of the link data structure which can be used for this connection.
	 * @param targetNicId The NIC ID of the receiver side.
	 * @param targetEndpoint The (native) endpoint id of the receiver side.
	 * @param filter The filter which should be used for this connection.
	 * @param timeout The timeout in milliseconds.
	 * @return true when succesful, false when a timeout occurs, or on any other error.
	 */
	static native boolean connect(int endpointId, int link, long targetNicId, int targetEndpoint, int filter, long timeout);
	
	/**
	 * Closes the connection.
	 * @param link The link datastructure of the connection that can be closed.
	 * @return true when succesful, false if the link does not exist (anymore).
	 */
	static native boolean disconnect(int link);

	/**
	 * Initiates an unreliable message transfer over a link. This request is succesful when the message is sent correctly.
	 * @param buffer A buffer containing the message
	 * @param offset The offset of the message in the buffer.
	 * @param msgSize The size of the message in bytes.
	 * @param endpointId The endpoint that will be used to send the message. 
	 * @param link The link over which the message will be sent.
	 * @param handle The request handle that can be used for this operation.
	 * @param matchData The matching data.
	 */
	static native void send(ByteBuffer buffer, int offset, int msgSize, int endpointId, int link, int handle, long matchData);
	/**
	 * Initiates a reliable message transfer over a link. This request is succesful when the message is received correctly.
	 * @param buffer A buffer containing the message.
	 * @param offset The offset of the message in the buffer.
	 * @param msgSize The size of the message in bytes.
	 * @param endpointId The endpoint that will be used to send the message. 
	 * @param link The link over which the message will be sent.
	 * @param handle The handle that can be used for this request.
	 * @param matchData The matching data.
	 */
	static native void sendSynchronous(ByteBuffer buffer, int offset, int msgSize, int endpointId, int link, int handle, long matchData); // returns a handle
	
	/**
	 * Receives a message from an endpoint. Only message with correct matching data will be received.
	 * @param buffer The buffer in to which the message will be written.
	 * @param offset The offset in the buffer where the message will be written at.
	 * @param bufsize The maximum number of bytes that can be written to the buffer.
	 * @param endpointId The local endpoint at which the message must arrive.
	 * @param handle The handle that can be used for this request.
	 * @param matchData The matching data. Only messages with exactly this matching data will be received by this request.
	 * @throws MxException 
	 */
	static native void recv(ByteBuffer buffer, int offset, int bufsize, int endpointId, int handle, long matchData) throws MxException; // returns a handle
	
	/**
	 * Receives a message from an endpoint. Only message with correct matching data will be received.
	 * @param buffer The buffer in to which the message will be written.
	 * @param offset The offset in the buffer where the message will be written at.
	 * @param bufsize The maximum number of bytes that can be written to the buffer.
	 * @param endpointId The local endpoint at which the message must arrive.
	 * @param handle The handle that can be used for this request.
	 * @param matchData The matching data. Only messages with matching data that equals this field after masking with the mask will be received by this request.
	 * @param matchMask The mask applied to the matching data of the message. 
	 * @throws MxException
	 */
	static native void recv(ByteBuffer buffer, int offset, int bufsize, int endpointId, int handle, long matchData, long matchMask) throws MxException; // returns a handle
	
	/**
	 * Waits for a request to finish.
	 * @param endpointId The local endpoint ID. 
	 * @param handle The handle of the request to wait for.
	 * @return The message size, or -1 when not successful.
	 * @throws MxException
	 */
	static native int wait(int endpointId, int handle) throws MxException; // return message size by success, -1, when unsuccessful

	/**
	 * Waits for a request to finish.
	 * @param endpointId The local endpoint ID. 
	 * @param handle The handle of the request to wait for.
	 * @param timeout The timeout in milliseconds.
	 * @return The message size, or -1 when not successful.
	 * @throws MxException
	 */
	static native int wait(int endpointId, int handle, long timeout) throws MxException; // return message size by success, -1, when unsuccessful
	
	/**
	 * Tests whether a request is finished.
	 * @param endpointId The local endpoint ID. 
	 * @param handle The handle of the request to test for.
	 * @return The message size, or -1 when not successful.
	 * @throws MxException
	 */
	static native int test(int endpointId, int handle) throws MxException; // return message size by success, -1, when unsuccessful
	
	/**
	 * Probes for a new message that is ready to be received
	 * @param endpointId The local endpoint ID.
	 * @param matchData The matching data. Only messages with matching data that equals this field after masking with the mask will be probed for by this request.
	 * @param matchMask The mask applied to the matching data of the message. 
	 * @return the size of the message that can be received, -1 when there is no message
	 */
	static native int iprobe(int endpointId, long matchData, long matchMask);
	
	/**
	 * Cancels a pending request
	 * @param endpointId The Id of the endpoint we are working on.
	 * @param handle The handle of a request that has to be canceled.
	 * @return True when ??? See MX documentation.
	 */
	static native boolean cancel(int endpointId, int handle);
	
	/**
	 * Wake up all threads that are blocked on the endpoint.
	 * @param endpointId The endpoint for which all threads will be waked up.
	 */
	static native void wakeup(int endpointId);
	
	
	
			
	
	//TODO do not use filters in Java, but move them away to the native code completely ?
	//TODO add endpoint info to links in LinkManager?
}

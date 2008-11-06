package ibis.ipl.impl.mx.channels;

public class ConnectionRequest {
	protected static final int PENDING = 0;
	protected static final int ACCEPTED = 1;
	protected static final int REJECTED = 2;
	
	
	private final MxAddress source;
	private final byte[] descriptor;
	
	protected int status = PENDING;
	protected byte[] replyMessage;
	protected int msgSize = 0;
	protected ChannelManager cm;
	
	protected ConnectionRequest(MxAddress source, byte[] descriptor) {
		this.source = source;
		this.descriptor = descriptor;
		replyMessage = new byte[MxSocket.MAX_CONNECT_MSG_SIZE];
	}

	public MxAddress getSourceAddress() {
		return source;
	}
	
	public byte[] getDescriptor() {
		return descriptor;
	}
	
	public void reject() {
		status = REJECTED;
	}
	
	protected void accept() {
		status = ACCEPTED;
	}
	
	/**
	 * Set a detailed reply message to the request. When the message is larger than 
	 * MxSocket.MAX_CONNECT_MSG_SIZE the message is truncated.
	 * @param message the message
	 */
	public void setReplyMessage(byte[] message) {
		if(message.length > this.replyMessage.length) {
			msgSize = this.replyMessage.length;
		} else {
			msgSize = message.length;
		}
		System.arraycopy(message, 0, replyMessage, 0, msgSize);
	}
}

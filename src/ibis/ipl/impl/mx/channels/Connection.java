package ibis.ipl.impl.mx.channels;

public final class Connection {
	public static final byte ACCEPT = 1;
	public static final byte REJECT = 2;

	private WriteChannel wc;
	private byte[] message;
	private byte reply;

	Connection(WriteChannel wc, byte reply, byte[] message) {
		this.message = message;
		this.wc = wc;
		this.reply = reply;
	}

	public WriteChannel getWriteChannel() {
		return wc;
	}

	public byte[] getReplyMessage() {
		return message;
	}

	public byte getReply() {
		return reply;
	}
}
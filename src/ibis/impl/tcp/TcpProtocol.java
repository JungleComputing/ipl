package ibis.ipl.impl.tcp;

interface TcpProtocol {
	static final byte NEW_RECEIVER = 1;
	static final byte NEW_MESSAGE = 2;
	static final byte FREE = 3;

	static final byte RECEIVER_ACCEPTED = 4;
	static final byte RECEIVER_DENIED   = 5;

	static final byte NEW_CONNECTION     = 6;
	static final byte EXISTING_CONNECTION = 7;



	static final byte REPLY = 127;
}

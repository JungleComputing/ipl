package ibis.ipl.impl.mx;

public interface MxProtocol {
	static final byte NEW_RECEIVER = 1;
    static final byte NEW_MESSAGE = 2;
    static final byte CLOSE_ALL_CONNECTIONS = 3;
    static final byte CLOSE_ONE_CONNECTION = 4;
}

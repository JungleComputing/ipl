package ibis.impl.nio;

interface NioProtocol {
    static final byte NEW_RECEIVER = 1;
    static final byte NEW_MESSAGE = 2;
    static final byte CLOSE_CONNECTION = 3;

    static final byte RECEIVER_DENIED   = 4;

    static final byte NEW_CONNECTION     = 5;
    static final byte EXISTING_CONNECTION = 6;
}

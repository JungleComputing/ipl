package ibis.impl.tcp;

interface TcpProtocol {
    static final byte NEW_RECEIVER = 1;

    static final byte NEW_MESSAGE = 2;

    static final byte CLOSE_ALL_CONNECTIONS = 3;

    static final byte CLOSE_ONE_CONNECTION = 4;

    static final byte RECEIVER_ACCEPTED = 5;

    static final byte RECEIVER_DENIED = 6;

    static final byte RECEIVER_DISABLED = 7;

    static final byte NEW_CONNECTION = 8;

    static final byte EXISTING_CONNECTION = 9;

    static final byte QUIT_IBIS = 10;

    static final byte REPLY = 127;
}
package ibis.impl.nio;

interface Protocol {
    static final byte NEW_RECEIVER = 1;
    static final byte NEW_MESSAGE = 2;

    static final byte CLOSE_ALL_CONNECTIONS = 3;
    static final byte CLOSE_ONE_CONNECTION = 4;

    static final byte CONNECTION_REQUEST = 5;
    static final byte CONNECTION_ACCEPTED = 6;
    static final byte CONNECTION_DENIED = 7;

    static final byte CONNECTIONS_DISABLED = 8;
}

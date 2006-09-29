/* $Id$ */

package ibis.impl.tcp;

interface TcpProtocol {
    static final byte NEW_RECEIVER = 1;

    static final byte NEW_MESSAGE = 2;

    static final byte CLOSE_ALL_CONNECTIONS = 3;

    static final byte CLOSE_ONE_CONNECTION = 4;

    static final byte RECEIVER_ACCEPTED = 7;

    static final byte RECEIVER_DENIED = 8;

    static final byte RECEIVER_DISABLED = 9;

    static final byte RECEIVER_ALREADYCONNECTED = 10;

    static final byte RECEIVER_TYPEMISMATCH = 11;

    static final byte NEW_CONNECTION = 12;

    static final byte EXISTING_CONNECTION = 13;

    static final byte QUIT_IBIS = 14;

    static final byte REPLY = 127;
}

/* $Id: Protocol.java 2844 2004-11-24 10:52:27Z ceriel $ */

package ibis.impl.nio;

interface Protocol {
    static final byte NEW_RECEIVER = 1;

    static final byte NEW_MESSAGE = 2;

    static final byte CLOSE_ALL_CONNECTIONS = 3;

    static final byte CLOSE_ONE_CONNECTION = 4;

    static final byte CONNECTION_REQUEST = 5;
}

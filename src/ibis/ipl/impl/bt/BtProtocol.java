/* $Id: TcpProtocol.java 6223 2007-09-05 09:33:35Z ceriel $ */

package ibis.ipl.impl.bt;

interface BtProtocol {
    static final byte NEW_RECEIVER = 1;

    static final byte NEW_MESSAGE = 2;

    static final byte CLOSE_ALL_CONNECTIONS = 3;

    static final byte CLOSE_ONE_CONNECTION = 4;

    static final byte NEW_CONNECTION = 12;

    static final byte EXISTING_CONNECTION = 13;

    static final byte QUIT_IBIS = 14;

    static final byte REPLY = 127;
}

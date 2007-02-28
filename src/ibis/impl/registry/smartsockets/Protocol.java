/* $Id: Protocol.java 5108 2007-02-27 15:35:17Z ceriel $ */

package ibis.impl.registry.smartsockets;

interface Protocol {

    static final byte IBIS_JOIN = 0;
    static final byte IBIS_REFUSED = 1;
    static final byte IBIS_ACCEPTED = 2;
    static final byte IBIS_MUSTLEAVE = 3;
    static final byte IBIS_LEAVE = 4;
    static final byte IBIS_PING = 5;
    static final byte IBIS_ISALIVE = 6;
    static final byte IBIS_DEAD = 7;
    static final byte IBIS_CHECK = 8;
    static final byte IBIS_CHECKALL = 9;

    static final byte SEQNO = 50;

    static final byte ELECTION_KILL = 98;
    static final byte ELECTION = 99;
    static final byte ELECTION_EXIT = 100;
}

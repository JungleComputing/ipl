package ibis.impl.nameServer.tcp;

interface Protocol {

    static final byte IBIS_JOIN = 0;
    static final byte IBIS_REFUSED = 1;
    static final byte IBIS_ACCEPTED = 2;
    static final byte IBIS_LEAVE = 4;
    static final byte IBIS_PING = 5;
    static final byte IBIS_ISALIVE = 6;
    static final byte IBIS_DEAD = 7;

    static final byte PORT_NEW = 20;
    static final byte PORT_ACCEPTED = 21;
    static final byte PORT_REFUSED = 22;
    static final byte PORT_LOOKUP = 23;
    static final byte PORT_LEAVE = 24;
    static final byte PORT_FREE = 25;
    static final byte PORT_KNOWN = 26;
    static final byte PORT_UNKNOWN = 27;
    static final byte PORT_EXIT = 28;
    static final byte PORT_REBIND = 29;
    static final byte PORT_LIST = 30;

    static final byte PORTTYPE_NEW = 40;
    static final byte PORTTYPE_ACCEPTED = 41;
    static final byte PORTTYPE_REFUSED = 42;
    static final byte PORTTYPE_EXIT = 43;

    static final byte SEQNO = 50;

    static final byte ELECTION_KILL = 98;
    static final byte ELECTION = 99;
    static final byte ELECTION_EXIT = 100;
}

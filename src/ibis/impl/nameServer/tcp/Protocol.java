package ibis.impl.nameServer.tcp;

interface Protocol { 

	static final byte 
		IBIS_JOIN     = 0, 
		IBIS_REFUSED  = 1, 
		IBIS_ACCEPTED = 2, 
		IBIS_LEAVE    = 4,
		IBIS_DELETE   = 5,
		IBIS_RECONFIGURE = 6,
		IBIS_PING     = 7,

		PORT_NEW      = 20, 
		PORT_ACCEPTED = 21, 
		PORT_REFUSED  = 22, 		
		PORT_LOOKUP   = 23, 
		PORT_LEAVE    = 24, /* a port is disconnected */
		PORT_FREE     = 25, /* a port is destroyed */
		PORT_KNOWN    = 26, 
		PORT_UNKNOWN  = 27,		
		PORT_EXIT     = 28,
		//gosia		
		PORT_REBIND   = 29,
		PORT_LIST     = 30,
		//end gosia

		PORTTYPE_NEW      = 40, 
		PORTTYPE_ACCEPTED = 41, 
		PORTTYPE_REFUSED  = 42,
                PORTTYPE_EXIT     = 43,
		SEQNO		  = 50,

		REELECTION    = 98,
	        ELECTION      = 99,
		ELECTION_EXIT      = 100;	
} 

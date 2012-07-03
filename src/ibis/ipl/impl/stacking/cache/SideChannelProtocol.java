package ibis.ipl.impl.stacking.cache;

interface SideChannelProtocol {
    
//    static final byte RESERVE_RP = 0;
    
    static final byte ACK = 0;
    
    static final byte CACHE_FROM_RP_AT_SP = 1;
    
    static final byte CACHE_FROM_SP = 2;
    
    static final byte DISCONNECT = 4;
}

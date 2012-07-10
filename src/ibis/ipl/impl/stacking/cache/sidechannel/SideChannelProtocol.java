package ibis.ipl.impl.stacking.cache.sidechannel;

public interface SideChannelProtocol {
    
    public static final byte ACK = 0;
    
    public static final byte CACHE_FROM_RP_AT_SP = 1;
    
    public static final byte CACHE_FROM_SP = 2;
    
    public static final byte DISCONNECT = 3;
    
    public static final byte READ_MY_MESSAGE = 4;
    
    public static final byte GIVE_ME_YOUR_MESSAGE = 5;
}

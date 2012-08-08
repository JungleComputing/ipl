package ibis.ipl.impl.stacking.cc.sidechannel;

public interface SideChannelProtocol {
    
    public static final byte RESERVE = 0;
    
    public static final byte RESERVE_ACK = 1;
    
    public static final byte RESERVE_NACK = 2;
    
    public static final byte CANCEL_RESERVATION = 3;

    public static final byte CACHE_FROM_RP_AT_SP = 8;
    
    public static final byte CACHE_FROM_SP = 16;
    
    public static final byte CACHE_FROM_SP_ACK = 17;
    
    public static final byte DISCONNECT = 32;
    
    public static final byte READ_MY_MESSAGE = 64;
    
    public static final byte GIVE_ME_YOUR_MESSAGE = 65;
}

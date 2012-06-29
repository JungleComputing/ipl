package ibis.ipl.impl.stacking.cache;

import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import java.io.IOException;

public class SideChannelMessageUpcall implements MessageUpcall, SideChannelProtocol {

    CacheManager cache;

    SideChannelMessageUpcall(CacheManager cache) {
        this.cache = cache;
    }

    @Override
    public void upcall(ReadMessage msg) throws IOException, ClassNotFoundException {
        byte opcode = msg.readByte();
        
        SendPortIdentifier spi;
        ReceivePortIdentifier rpi;

        switch (opcode) {
            /*
             * The sender machine wants a free port at this machine.
             * 
             * useless: with connection_upcalls, when a sendport connects,
             * the receiveport will get a gotConnection upcall.
             */
            case RESERVE_RP:
//                cache.reserve(msg.origin().ibisIdentifier());
                break;
            /*
             * This upcall comes when the sending machine wants to
             * cache a connection from this sendport to its receiveport.
             * 
             * not useless.
             */
            case CACHE_SP:
                spi = (SendPortIdentifier) msg.readObject();
                rpi = (ReceivePortIdentifier) msg.readObject();
                cache.cache(spi, rpi);
                break;
            /*
             * This upcall comes when the sendport cached the connection.
             * Need to count it at the receive port as well.
             * 
             * useless: if i have connection_upcalls, 
             * the receive port will get an upcall for the lost connection.
             */
            case CACHE_RP:
//                spi = (SendPortIdentifier) msg.readObject();
//                rpi = (ReceivePortIdentifier) msg.readObject();
//                cache.alreadyCached(spi, rpi);
                break;
        }
    }
}

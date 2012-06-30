package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;

public class SideChannelMessageUpcall implements MessageUpcall, SideChannelProtocol {

    final CacheManager cacheManager;
    public static final Object ackLock = new Object();
    public static boolean ackReceived = false;

    SideChannelMessageUpcall(CacheManager cache) {
        this.cacheManager = cache;        
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
             * useless: with connection_upcalls, when a sendport connects, the
             * receiveport will get a gotConnection upcall.
             */
//            case RESERVE_RP:
//                cache.reserve(msg.origin().ibisIdentifier());
//                break;
            /*
             * This upcall comes when the sending machine wants to cache a
             * connection from this sendport to its receiveport.
             */
            case CACHE_FROM_RP_AT_SP:
                spi = (SendPortIdentifier) msg.readObject();
                rpi = (ReceivePortIdentifier) msg.readObject();
                synchronized (cacheManager) {
                    cacheManager.removeConnection(spi, rpi);
                }
                break;

            /*
             * This upcall comes when the sendport cached the connection. The
             * actual disconnection will take place at the lostConnection()
             * upcall. Here we merely want to mark that the disconnect call to
             * come is caching and not a true disconnect call.
             */
            case CACHE_FROM_SP:
                spi = (SendPortIdentifier) msg.readObject();
                rpi = (ReceivePortIdentifier) msg.readObject();
                CacheReceivePort.map.get(rpi).futureCachedConnection(spi);

                /*
                 * Now send ack back.
                 */
                ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                        msg.origin().ibisIdentifier(), CacheManager.sideChnRPName);
                WriteMessage ack = cacheManager.sideChannelSendPort.newMessage();
                ack.writeByte(SideChannelProtocol.ACK);
                ack.finish();
                cacheManager.sideChannelSendPort.disconnect(sideRpi);

                // done
                break;

            case ACK:
                synchronized(ackLock) {
                    ackReceived = true;
                    ackLock.notifyAll();
                }
                break;
        }
    }
}

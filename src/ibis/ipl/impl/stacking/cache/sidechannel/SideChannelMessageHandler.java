package ibis.ipl.impl.stacking.cache.sidechannel;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SideChannelMessageHandler implements MessageUpcall, SideChannelProtocol {

    final CacheManager cacheManager;
    
    public static final Map<Byte, String> map = new HashMap<Byte, String>();
    
    static {
        map.put(RESERVE, "RESERVE");
        map.put(RESERVE_ACK, "RESERVE_ACK");
        map.put(CANCEL_RESERVATION, "CANCEL_RESERVATION");
        map.put(CACHE_FROM_RP_AT_SP, "CACHE_FROM_RP_AT_SP");
        map.put(CACHE_FROM_SP, "CACHE_FROM_SP");
        map.put(CACHE_FROM_SP_ACK, "CACHE_FROM_SP_ACK");
        map.put(DISCONNECT, "DISCONNECT");
        map.put(READ_MY_MESSAGE, "READ_MY_MESSAGE");
        map.put(GIVE_ME_YOUR_MESSAGE, "GIVE_ME_YOUR_MESSAGE");
    }

    public SideChannelMessageHandler(CacheManager cache) {
        this.cacheManager = cache;
    }

    @Override
    public void upcall(ReadMessage msg) throws IOException, ClassNotFoundException {
        byte opcode = msg.readByte();
        SendPortIdentifier spi = (SendPortIdentifier) msg.readObject();
        ReceivePortIdentifier rpi = (ReceivePortIdentifier) msg.readObject();
        msg.finish();

        CacheReceivePort rp = CacheReceivePort.map.get(rpi);
        CacheSendPort sp = CacheSendPort.map.get(spi);

        CacheManager.log.log(Level.INFO, "\tGot side-message: \t["
                + "({0}-{1}, {2}-{3}), OPCODE = {4}]",
                new Object[]{spi.name(), spi.ibisIdentifier().name(),
                    rpi.name(), rpi.ibisIdentifier().name(), 
                    map.get(opcode)});

        switch (opcode) {
            /*
             * At ReceivePortSide: the sender machine will establish a future
             * connection. Make room for it.
             */
            case RESERVE:
                cacheManager.lock.lock();
                try {
                    cacheManager.reserveConnection(rpi, spi);
                } finally {
                    cacheManager.lock.unlock();
                }
                /*
                 * Now send ack back.
                 */
                newThreadSendProtocol(rpi, spi, SideChannelProtocol.RESERVE_ACK);
                break;
            /*
             * At SendPortSide: the ack received after our request for a
             * connection.
             */
            case RESERVE_ACK:
                cacheManager.lock.lock();
                try {
                    sp.reserveAckReceived.add(rpi);
                    cacheManager.reserveAckCond.signal();
                } finally {
                    cacheManager.lock.unlock();
                }
                break;

            /*
             * At ReceivePortSide:
             * The promised connection will arive no more.
             */
            case CANCEL_RESERVATION:
                cacheManager.lock.lock();
                try {
                    cacheManager.cancelReservation(rpi, spi);
                } finally {
                    cacheManager.lock.unlock();
                }
                break;
            /*
             * At SendPortSide: This upcall comes when the receive port at the
             * sending machine wants to cache a connection from this sendport to
             * its receiveport.
             */
            case CACHE_FROM_RP_AT_SP:
                cacheManager.lock.lock();
                try {
                    boolean heKnows = true;
                    CacheSendPort.map.get(spi).cache(rpi, heKnows);
                    cacheManager.cacheConnection(spi, rpi);
                } finally {
                    cacheManager.lock.unlock();
                }
                break;

            /*
             * At ReceivePortSide: This upcall comes when the sendport caches
             * the connection. The actual disconnection will take place at the
             * lostConnection() upcall. Here we merely want to mark that the
             * disconnect call to come is caching and not a true disconnect
             * call.
             */
            case CACHE_FROM_SP:
                CacheReceivePort.map.get(rpi).toBeCachedSet.add(spi);

                /*
                 * Now send ack back.
                 */
                newThreadSendProtocol(rpi, spi, SideChannelProtocol.CACHE_FROM_SP_ACK);

                /*
                 * Done.
                 */
                break;

            /*
             * At SendPortSide: Ack received from the above scenario.
             */
            case CACHE_FROM_SP_ACK:
                synchronized (sp.cacheAckLock) {
                    sp.cacheAckReceived = true;
                    sp.cacheAckLock.notifyAll();
                }
                break;

            /*
             * At ReceivePortSide: This protocol is sent through the side
             * channel because the real connection has been cached. - no point
             * in turning it up again only to have it closed, right?
             */
            case DISCONNECT:
                rp.connectUpcall.lostConnection(null, spi, null);
                break;

            /*
             * At ReceivePortSide: the send port which sent this protocol
             * requests that we will read his message. Place it in the waiting
             * queue, because we might have now some other alive read message.
             */
            case READ_MY_MESSAGE:
                synchronized (rp) {
                    /*
                     * If I have a current alive message OR if I already gave
                     * permision to another send port, then store this request
                     * for later.
                     */
                    if ((rp.currentReadMsg != null)
                            || rp.readMsgRequested) {
                        rp.toHaveMyFutureAttention.add(spi);
                        rp.notifyAll();
                    } else {
                        /*
                         * Set to false when we actually receive the message.
                         */
                        rp.readMsgRequested = true;
                        newThreadSendProtocol(rpi, spi, GIVE_ME_YOUR_MESSAGE);
                    }
                }
                break;

            /*
             * At SendPortSide: the receive port which sent this protocol will
             * enable its aliveReadMessage from this sendport. Notify the
             * sendport of this so it can start writting to it.
             */
            case GIVE_ME_YOUR_MESSAGE:
                synchronized (sp.currentMsg.dataOut.yourReadMessageIsAliveFromMeSet) {
                    sp.currentMsg.dataOut.yourReadMessageIsAliveFromMeSet.add(rpi);
                    sp.currentMsg.dataOut.yourReadMessageIsAliveFromMeSet.notifyAll();
                }
                break;
        }
    }

    private void sendProtocol(SendPortIdentifier spi,
            ReceivePortIdentifier rpi, IbisIdentifier destination, byte opcode) {
        CacheManager.log.log(Level.INFO, "\tSending side-message: \t["
                + "({0}-{1}, {2}-{3}), OPCODE = {4}]",
                new Object[]{spi.name(), spi.ibisIdentifier().name(),
                    rpi.name(), rpi.ibisIdentifier().name(), 
                    map.get(opcode)});
        /*
         * Synchronize on the sideChannelSendPort so as not to send multiple
         * messages at the same time.
         */
        synchronized (cacheManager.sideChannelSendPort) {
            try {
                ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                        destination, CacheManager.sideChnRPName);
                WriteMessage msg = cacheManager.sideChannelSendPort.newMessage();
                msg.writeByte(opcode);
                msg.writeObject(spi);
                msg.writeObject(rpi);
                msg.finish();
                cacheManager.sideChannelSendPort.disconnect(sideRpi);
            } catch (Exception ex) {
                CacheManager.log.log(Level.SEVERE,
                        "Error at side channel:\n{0}", ex.toString());
            }
        }
    }

    public void newThreadSendProtocol(final ReceivePortIdentifier rpi,
            final SendPortIdentifier spi, final byte opcode) {

        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, spi.ibisIdentifier(), opcode);
            }
        }.start();
    }

    public void newThreadSendProtocol(final SendPortIdentifier spi,
            final ReceivePortIdentifier rpi, final byte opcode) {

        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode);
            }
        }.start();
    }

    public void sendProtocol(SendPortIdentifier spi, ReceivePortIdentifier rpi, byte opcode) {
        sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode);
    }
}

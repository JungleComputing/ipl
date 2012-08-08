package ibis.ipl.impl.stacking.cache.sidechannel;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.CacheReceivePort;
import ibis.ipl.impl.stacking.cache.CacheSendPort;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.util.Loggers;
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
        map.put(RESERVE_NACK, "RESERVE_NACK");
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
        long seqNo = -1;        
        if(opcode == READ_MY_MESSAGE) {
            seqNo = msg.readLong();
        }
        msg.finish();

        CacheReceivePort rp = CacheReceivePort.map.get(rpi);
        CacheSendPort sp = CacheSendPort.map.get(spi);

        Loggers.sideLog.log(Level.INFO, "\tGot side-message: \t["
                + "({0}-{1}, {2}-{3}), OPCODE = {4}]",
                new Object[]{spi.name(), spi.ibisIdentifier().name(),
                    rpi.name(), rpi.ibisIdentifier().name(), 
                    map.get(opcode)});

        switch (opcode) {
            /*
             * At ReceivePortSide: the sender machine will establish a future
             * connection. Make room for it if possible.
             */
            case RESERVE:
                byte answer = RESERVE_NACK;
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in RESERVE");
                try {
                    /*
                     * If I'm not full, or if I am but I can cache something,
                     * then I'm good to go.
                     */
                    if(!cacheManager.fullConns() ||
                            cacheManager.canCache()) {
                        cacheManager.reserveConnection(rpi, spi);
                        answer = RESERVE_ACK;
                    }                    
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock unlocked in RESERVE");
                }
                /*
                 * Now send ack/nack back.
                 */
                newThreadSendProtocol(rpi, spi, answer);
                break;
            /*
             * At SendPortSide: the ack received after our request for a
             * connection.
             */
            case RESERVE_ACK:
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in RESERVE_ACK");
                try {
                    sp.reserveAcks.put(rpi, RESERVE_ACK);
                    cacheManager.reserveAcksCond.signalAll();
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock unlocked in RESERVE_ACK");
                }
                break;
            /*
             * At SendPortSide: the negative ack received after our request
             * for a connection.
             */    
            case RESERVE_NACK:
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in RESERVE_NACK");
                try {
                    cacheManager.cancelReservation(spi, rpi);
                    
                    sp.reserveAcks.put(rpi, RESERVE_NACK);
                    cacheManager.reserveAcksCond.signalAll();
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock unlocked in RESERVE_NACK");
                }
                break;

            /*
             * At ReceivePortSide:
             * The promised connection will arive no more.
             */
            case CANCEL_RESERVATION:
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in CANCEL_RESERVATION");
                try {
                    cacheManager.cancelReservation(rpi, spi);
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock unlocked in CANCEL_RESERVATION");
                }
                break;
            /*
             * At SendPortSide: This upcall comes when the receive port at the
             * sending machine wants to cache a connection from this sendport to
             * its receiveport.
             */
            case CACHE_FROM_RP_AT_SP:
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in CACHE_FROM_RP_AT_SP");
                try {
                    boolean heKnows = true;
                    /*
                     * It may be that this connection is already
                     * cached or even closed if a caching/disconnection
                     * came from the SendPort simultaneously (but a bit faster).
                     */
                    if(cacheManager.isConnAlive(spi, rpi)) {
                        cacheManager.cacheConnection(spi, rpi, heKnows);
                    } else if(!cacheManager.isConnCached(spi, rpi)) {
                        /*
                         * It is not alive and not even cached.
                         * The sendport closed the connection while
                         * the receive port wanted it cached.
                         * 
                         * The ReceivePort thinks the lostConnection() upcall 
                         * he got is a connection caching, but it
                         * actually is a real disconnect call.
                         * 
                         * send him a disconnect message to wake him up.
                         */
                        cacheManager.sideChannelHandler.sendProtocol(spi,
                                rpi, SideChannelProtocol.DISCONNECT);
                    }
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock unlocked in CACHE_FROM_RP_AT_SP");
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
                cacheManager.lock.lock();
                Loggers.lockLog.log(Level.INFO, "Lock locked in CACHE_FROM_SP");
                try {
                    if (cacheManager.isConnAlive(rpi, spi)) {

                        CacheReceivePort.map.get(rpi).toBeCachedSet.add(spi);

                        /*
                         * Move the alive connection to a reserved spot, so
                         * nobody will try to cache this connection, since it
                         * will already be cached.
                         */
                        cacheManager.reserveLiveConnection(rpi, spi);
                    }

                    /*
                     * Now send ack back.
                     */
                    newThreadSendProtocol(rpi, spi, SideChannelProtocol.CACHE_FROM_SP_ACK);
                } finally {
                    cacheManager.lock.unlock();
                    Loggers.lockLog.log(Level.INFO, "Lock released in CACHE_FROM_SP");
                }
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
                            || rp.readMsgRequested
                            || !rp.isNextSeqNo(seqNo)) {

                        if (rp.currentReadMsg != null) {
                            Loggers.readMsgLog.log(Level.INFO, "I have a current alive"
                                    + " read message, and I will handle this"
                                    + " message later.");
                        }
                        if (rp.readMsgRequested) {
                            Loggers.readMsgLog.log(Level.INFO, "I have "
                                    + "requested a read message from someone, "
                                    + "and I will handle this"
                                    + " message later.");
                        }

                        rp.toHaveMyFutureAttention.add(
                                new CacheReceivePort.SequencedSpi(seqNo, spi));
                        rp.notifyAll();
                    } else {
                        /*
                         * Set to false when we actually receive the message.
                         */
                        rp.readMsgRequested = true;
                        rp.incSeqNo(seqNo);
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
                synchronized (sp.dataOut.yourReadMessageIsAliveFromMeSet) {
                    sp.dataOut.yourReadMessageIsAliveFromMeSet.add(rpi);
                    sp.dataOut.yourReadMessageIsAliveFromMeSet.notifyAll();
                }
                break;
        }
    }

    private void sendProtocol(SendPortIdentifier spi,
            ReceivePortIdentifier rpi, IbisIdentifier destination, byte opcode,
            long seqNo) {        
        /*
         * Synchronize on the sideChannelSendPort so as not to send multiple
         * messages at the same time.
         */
        synchronized (cacheManager.sideChannelSendPort) {
            Loggers.sideLog.log(Level.INFO, "\tSending side-message: \t["
                    + "({0}-{1}, {2}-{3}), OPCODE = {4}"
                    + (opcode == READ_MY_MESSAGE ? ", seqNo = {5}]" : "]"),
                    new Object[]{spi.name(), spi.ibisIdentifier().name(),
                        rpi.name(), rpi.ibisIdentifier().name(),
                        map.get(opcode), seqNo});
            try {
                ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                        destination, CacheManager.sideChnRPName);
                WriteMessage msg = cacheManager.sideChannelSendPort.newMessage();
                msg.writeByte(opcode);
                msg.writeObject(spi);
                msg.writeObject(rpi);
                
                if(opcode == READ_MY_MESSAGE) {
                    msg.writeLong(seqNo);
                }
                
                msg.finish();
                cacheManager.sideChannelSendPort.disconnect(sideRpi);
            } catch (Exception ex) {
                Loggers.sideLog.log(Level.SEVERE,
                        "Error at side channel:\n{0}", ex.toString());
            }
        }
    }

    public void newThreadSendProtocol(final ReceivePortIdentifier rpi,
            final SendPortIdentifier spi, final byte opcode) {

        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, spi.ibisIdentifier(), opcode, -1);
            }
        }.start();
    }

    public void newThreadSendProtocol(final SendPortIdentifier spi,
            final ReceivePortIdentifier rpi, final byte opcode) {

        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, -1);
            }
        }.start();
    }
    
    public void newThreadRMMProtocol(final SendPortIdentifier spi,
            final ReceivePortIdentifier rpi, final byte opcode, final long seq) {
        assert opcode == READ_MY_MESSAGE;
        
        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, seq);
            }
        }.start();
    }
    
    public void sendProtocol(SendPortIdentifier spi, ReceivePortIdentifier rpi, byte opcode) {
        sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, -1);
    }
}

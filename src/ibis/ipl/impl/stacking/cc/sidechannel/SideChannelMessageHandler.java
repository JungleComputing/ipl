package ibis.ipl.impl.stacking.cc.sidechannel;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cc.CCReceivePort;
import ibis.ipl.impl.stacking.cc.CCSendPort;
import ibis.ipl.impl.stacking.cc.manager.CCManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SideChannelMessageHandler implements MessageUpcall, SideChannelProtocol {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(SideChannelMessageHandler.class);

    final CCManager ccManager;
    
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
    
    public int noOfSideMsgsToSend;

    public SideChannelMessageHandler(CCManager manager) {
        this.ccManager = manager;
        noOfSideMsgsToSend = 0;        
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

        CCReceivePort rp = CCReceivePort.map.get(rpi);
        CCSendPort sp = CCSendPort.map.get(spi);

        logger.debug("\tGot side-message: \t["
                + "({}-{}, {}-{}), OPCODE = {}"
                + (opcode == READ_MY_MESSAGE ? ", seqNo = {}]" : "]"),
                new Object[]{spi.name(), spi.ibisIdentifier().name(),
                    rpi.name(), rpi.ibisIdentifier().name(), 
                    map.get(opcode), seqNo});

        switch (opcode) {
            /*
             * At ReceivePortSide: the sender machine will establish a future
             * connection. Make room for it if possible.
             */
            case RESERVE:
                byte answer = RESERVE_NACK;
                ccManager.lock.lock();
                logger.debug("Lock locked in RESERVE");
                try {
                    /*
                     * If I'm not full, or if I am but I can cache something,
                     * then I'm good to go.
                     */
                    if (!ccManager.fullConns()
                            || ccManager.canCache()) {
                        ccManager.reserveConnection(rpi, spi);
                        answer = RESERVE_ACK;
                    }
                    if (answer == RESERVE_ACK) {
                        logger.debug("Reserving connection ({}, {}).",
                                new Object[]{spi, rpi});
                    } else {
                        logger.debug("Denying reservation for"
                                + " connection ({}, {}).",
                                new Object[]{spi, rpi});
                    }
                    /*
                     * Now send ack/nack back.
                     */
                    newThreadSendProtocol(rpi, spi, answer);
                } finally {
                    logger.debug("Unlocking lock in RESERVE");
                    ccManager.lock.unlock();                    
                }
                break;
            /*
             * At SendPortSide: the ack received after our request for a
             * connection.
             */
            case RESERVE_ACK:
                ccManager.lock.lock();
                logger.debug("Lock locked in RESERVE_ACK");
                try {
                    sp.reserveAcks.put(rpi, RESERVE_ACK);
                    ccManager.reserveAcksCond.signalAll();
                } finally {
                    logger.debug("Unlocking lock in RESERVE_ACK");
                    ccManager.lock.unlock();                    
                }
                break;
            /*
             * At SendPortSide: the negative ack received after our request
             * for a connection.
             */    
            case RESERVE_NACK:
                ccManager.lock.lock();
                logger.debug("Lock locked in RESERVE_NACK");
                try {
                    ccManager.cancelReservation(spi, rpi);
                    
                    sp.reserveAcks.put(rpi, RESERVE_NACK);
                    ccManager.reserveAcksCond.signalAll();
                } finally {
                    logger.debug("Unlocking lock in RESERVE_NACK");
                    ccManager.lock.unlock();                    
                }
                break;

            /*
             * At ReceivePortSide:
             * The promised connection will arive no more.
             */
            case CANCEL_RESERVATION:
                ccManager.lock.lock();
                logger.debug("Lock locked in CANCEL_RESERVATION");
                try {
                    ccManager.cancelReservation(rpi, spi);
                } finally {
                    logger.debug("Unlocking lock in CANCEL_RESERVATION");
                    ccManager.lock.unlock();                    
                }
                break;
            /*
             * At SendPortSide: This upcall comes when the receive port at the
             * sending machine wants to cache a connection from this sendport to
             * its receiveport.
             */
            case CACHE_FROM_RP_AT_SP:
                ccManager.lock.lock();
                logger.debug("Lock locked in CACHE_FROM_RP_AT_SP");
                try {
                    boolean heKnows = true;
                    /*
                     * It may be that this connection is already
                     * cached or even closed if a caching/disconnection
                     * came from the SendPort simultaneously (but a bit faster).
                     */
                    if(ccManager.isConnAlive(spi, rpi)) {
                        ccManager.cacheConnection(spi, rpi, heKnows);
                    } else if(!ccManager.isConnCached(spi, rpi)) {
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
                        ccManager.sideChannelHandler.sendProtocol(spi,
                                rpi, SideChannelProtocol.DISCONNECT);
                    }
                } finally {
                    logger.debug("Unlocking lock in CACHE_FROM_RP_AT_SP");
                    ccManager.lock.unlock();                    
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
                ccManager.lock.lock();
                logger.debug("Lock locked in CACHE_FROM_SP");
                try {
                    if (ccManager.isConnAlive(rpi, spi)) {

                        CCReceivePort.map.get(rpi).toBeCachedSet.add(spi);

                        /*
                         * Move the alive connection to a reserved spot, so
                         * nobody will try to cache this connection, since it
                         * will already be cached.
                         */
                        ccManager.reserveLiveConnection(rpi, spi);
                    }

                    /*
                     * Now send ack back.
                     */
                    newThreadSendProtocol(rpi, spi, SideChannelProtocol.CACHE_FROM_SP_ACK);
                } finally {
                    logger.debug("Releasing lock in CACHE_FROM_SP");
                    ccManager.lock.unlock();                    
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
                    if ((rp.currentLogicalReadMsg != null)
                            || rp.readMsgRequested
                            || !rp.isNextSeqNo(seqNo)) {

                        if (rp.currentLogicalReadMsg != null) {
                            logger.debug("I have a current alive"
                                    + " read message, and I will handle this"
                                    + " message later.");
                        }
                        if (rp.readMsgRequested) {
                            logger.debug("I have "
                                    + "requested a read message from someone, "
                                    + "and I will handle this"
                                    + " message later.");
                        }
                        if (!rp.isNextSeqNo(seqNo)) {
                            logger.debug("I have to get another message"
                                    + " before I can get this one. ");
                        }

                        rp.toHaveMyFutureAttention.add(
                                new CCReceivePort.SequencedSpi(seqNo, spi));
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
         * Synchronize on the sideChannelSendPort so as not to connect to
         * multiple receive ports at the same time.
         */
        synchronized (ccManager.sideChannelSendPort) {
            logger.debug("\tSending side-message: \t["
                    + "({}-{}, {}-{}), OPCODE = {}"
                    + (opcode == READ_MY_MESSAGE ? ", seqNo = {}]" : "]"),
                    new Object[]{spi.name(), spi.ibisIdentifier().name(),
                        rpi.name(), rpi.ibisIdentifier().name(),
                        map.get(opcode), seqNo});
            try {
                ReceivePortIdentifier sideRpi = ccManager.sideChannelSendPort.connect(
                        destination, CCManager.sideChnRPName);
                WriteMessage msg = ccManager.sideChannelSendPort.newMessage();
                msg.writeByte(opcode);
                msg.writeObject(spi);
                msg.writeObject(rpi);
                
                if(opcode == READ_MY_MESSAGE) {
                    msg.writeLong(seqNo);
                }
                
                msg.finish();
                ccManager.sideChannelSendPort.disconnect(sideRpi);
            } catch (Throwable t) {
                logger.error("Error at side channel: ", t);
            }
        }
    }

    public void newThreadSendProtocol(final ReceivePortIdentifier rpi,
            final SendPortIdentifier spi, final byte opcode) {
        
        synchronized(this) {
            noOfSideMsgsToSend++;
        }

        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, spi.ibisIdentifier(), opcode, -1);
            }
        }.start();
        
        synchronized(this) {
            noOfSideMsgsToSend--;
            this.notify();
        }
    }

    public void newThreadSendProtocol(final SendPortIdentifier spi,
            final ReceivePortIdentifier rpi, final byte opcode) {
        
        synchronized(this) {
            noOfSideMsgsToSend++;
        }
        
        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, -1);
            }
        }.start();
        
        synchronized(this) {
            noOfSideMsgsToSend--;
            this.notify();
        }
    }
    
    public void newThreadRMMProtocol(final SendPortIdentifier spi,
            final ReceivePortIdentifier rpi, final byte opcode, final long seq) {
        assert opcode == READ_MY_MESSAGE;
        
        synchronized(this) {
            noOfSideMsgsToSend++;
        }
        
        new Thread() {

            @Override
            public void run() {
                sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, seq);
            }
        }.start();
        
        synchronized(this) {
            noOfSideMsgsToSend--;
            this.notify();
        }
    }
    
    public void sendProtocol(SendPortIdentifier spi, ReceivePortIdentifier rpi, byte opcode) {
        sendProtocol(spi, rpi, rpi.ibisIdentifier(), opcode, -1);
    }
}

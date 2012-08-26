package ibis.ipl.impl.stacking.cc;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendPortDisconnectUpcaller implements SendPortDisconnectUpcall {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(SendPortDisconnectUpcaller.class);

    private final CCSendPort port;
    private final SendPortDisconnectUpcall upcaller;

    public SendPortDisconnectUpcaller(SendPortDisconnectUpcall upcaller,
            CCSendPort port) {
        this.port = port;
        this.upcaller = upcaller;
    }

    @Override
    public void lostConnection(SendPort sendPort, ReceivePortIdentifier rpi,
            Throwable cause) {

        if(logger.isDebugEnabled()) {
        logger.debug("\n\tGot lost connection at send port...");
        logger.debug("\tcause was:\t", cause);
        }

        port.ccManager.lock.lock();
        if(logger.isDebugEnabled()) {
        logger.debug("Lock locked.");
        }
        try {
            port.ccManager.lostConnection(sendPort.identifier(), rpi);
        } finally {
            if(logger.isDebugEnabled()) {
            logger.debug("Unlocking lock.");
            }
            port.ccManager.lock.unlock();            
        }

        if (upcaller != null) {
            upcaller.lostConnection(port, rpi, cause);
        }
    }
}

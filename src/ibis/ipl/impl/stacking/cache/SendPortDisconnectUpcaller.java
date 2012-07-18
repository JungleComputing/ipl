package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import java.util.logging.Level;

public class SendPortDisconnectUpcaller implements SendPortDisconnectUpcall {

    private final CacheSendPort port;
    private final SendPortDisconnectUpcall upcaller;

    public SendPortDisconnectUpcaller(SendPortDisconnectUpcall upcaller,
            CacheSendPort port) {
        this.port = port;
        this.upcaller = upcaller;
    }

    @Override
    public void lostConnection(SendPort sendPort, ReceivePortIdentifier rpi,
            Throwable cause) {

        Loggers.conLog.log(Level.INFO, "\n\tGot lost connection at send port...");
        Loggers.conLog.log(Level.INFO, "\tcause was:\n{0}", cause);

        port.cacheManager.lock.lock();
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        try {
            port.cacheManager.lostConnection(sendPort.identifier(), rpi);
        } finally {
            port.cacheManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
        }

        if (upcaller != null) {
            upcaller.lostConnection(port, rpi, cause);
        }
    }
}

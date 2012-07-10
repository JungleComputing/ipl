package ibis.ipl.impl.stacking.cache;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
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

        CacheManager.log.log(Level.INFO, "\n\tGot lost connection at send port...");
        CacheManager.log.log(Level.INFO, "\tcause was:\n{0}", cause);

        synchronized (port.cacheManager) {
            port.cacheManager.removeConnection(sendPort.identifier(), rpi);
            port.cacheManager.notifyAll();
        }

        if (upcaller != null) {
            upcaller.lostConnection(port, rpi, cause);
        }
    }
}

package ibis.ipl.impl.stacking.cc;

import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import java.util.logging.Level;

public class SendPortDisconnectUpcaller implements SendPortDisconnectUpcall {

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

        Loggers.conLog.log(Level.INFO, "\n\tGot lost connection at send port...");
        Loggers.conLog.log(Level.INFO, "\tcause was:\t", cause);

        port.ccManager.lock.lock();
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        try {
            port.ccManager.lostConnection(sendPort.identifier(), rpi);
        } finally {
            Loggers.lockLog.log(Level.INFO, "Unlocking lock.");
            port.ccManager.lock.unlock();            
        }

        if (upcaller != null) {
            upcaller.lostConnection(port, rpi, cause);
        }
    }
}

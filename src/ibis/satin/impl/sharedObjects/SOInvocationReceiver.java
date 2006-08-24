/*
 * Created on May 3, 2006 by rob
 */
package ibis.satin.impl.sharedObjects;

import mcast.object.ObjectMulticaster;
import ibis.satin.SharedObject;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;

final class SOInvocationReceiver extends Thread implements Config {
    private Satin s;

    private ObjectMulticaster omc;

    protected SOInvocationReceiver(Satin s, ObjectMulticaster omc) {
        this.s = s;
        this.omc = omc;
        setName("SOInvocationReceiver");
    }

    public void run() {
        while (true) {
            try {
                Object o = omc.receive();

                if (o instanceof SOInvocationRecord) {
                    SOInvocationRecord soir = (SOInvocationRecord) o;
                    soLogger.debug("SATIN '" + s.ident.name() + "': "
                        + "received SO invocation broadcast id = "
                        + soir.getObjectId());
                    s.so.addSOInvocation(soir);
                } else if (o instanceof SharedObject) {
                    SharedObject obj = (SharedObject) o;
                    soLogger.debug("SATIN '" + s.ident.name() + "': "
                        + "received broadcast object, id = " + obj.objectId);
                    s.so.addObject(obj);
                } else {
                    soLogger
                        .warn("received unknown object in SOInvocation receiver");
                }
            } catch (Exception e) {
                soLogger.warn("WARNING, SOI Mcast receive failed: " + e, e);
            }
        }
    }
}

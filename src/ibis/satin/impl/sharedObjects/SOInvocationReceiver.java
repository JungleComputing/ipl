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
        setDaemon(true);
    }

    public void run() {
        while (true) {
            try {
                Object o = omc.receive();

                if (o instanceof SOInvocationRecord) {
                    SOInvocationRecord soir = (SOInvocationRecord) o;
                    soLogger.debug("SATIN '" + s.ident + "': "
                        + "received SO invocation broadcast id = "
                        + soir.getObjectId());
                    s.so.addSOInvocation(soir);
                } else if (o instanceof SharedObject) {
                    SharedObject obj = (SharedObject) o;
                    soLogger.debug("SATIN '" + s.ident + "': "
                        + "received broadcast object, id = " + obj.getObjectId());
                    s.so.addObject(obj);
                } else if (o != null) {
                    soLogger
                        .warn("received unknown object in SOInvocation receiver");
                }
            } catch (mcast.object.DoneException e) {
                soLogger.info("SOI Mcast receive thread done", e);
                return;
            } catch (Exception e2) {
                soLogger.warn("WARNING, SOI Mcast receive failed: ", e2);
            }
        }
    }
}

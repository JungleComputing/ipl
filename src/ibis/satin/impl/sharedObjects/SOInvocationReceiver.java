/*
 * Created on May 3, 2006 by rob
 */
package ibis.satin.impl.sharedObjects;

import mcast.object.ObjectMulticaster;
import ibis.satin.SharedObject;
import ibis.satin.impl.Satin;

final class SOInvocationReceiver extends Thread {
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
                    s.so.addSOInvocation(soir);
                } else if (o instanceof SharedObject) {
                    SharedObject obj = (SharedObject) o;
                    synchronized (s) {
                        s.so.sharedObjects.put(obj.objectId, obj);
                    }
                } else {
                    System.err.println("AAA");
                }
            } catch (Exception e) {
                System.err.println("WARNING, SOI Mcast receive failed: " + e);
                e.printStackTrace();
            }
        }
    }
}

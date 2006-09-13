/*
 * Created on May 3, 2006 by rob
 */
package ibis.satin.impl;

import ibis.satin.SharedObject;

public class SOInvocationReceiver extends Thread {
    Satin s;
    public SOInvocationReceiver(Satin s) {
        this.s = s;
    }
    
    public void run() {
        while (true) {
            try {

                Object o = s.omc.receive();
                
                if(o instanceof SOInvocationRecord) {
                    SOInvocationRecord soir = (SOInvocationRecord) o;
                    s.addSOInvocation(soir);
                } else if (o instanceof SharedObject) {
                    SharedObject obj = (SharedObject) o;
                    synchronized (s) {
                        s.sharedObjects.put(obj.objectId, obj);
                    }
                } else {
                    System.err.println("AAA");
                }
            } catch (Exception e) {
                System.err.println("WARNING, SOI Mcast receive failed: " + e);
            }
        }
    }
}

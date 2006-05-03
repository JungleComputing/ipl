/*
 * Created on May 3, 2006 by rob
 */
package ibis.satin.impl;

public class SOInvocationReceiver extends Thread {
    Satin s;
    public SOInvocationReceiver(Satin s) {
        this.s = s;
    }
    
    public void run() {
        while (true) {
            try {
                SOInvocationRecord soir = (SOInvocationRecord) s.omc.receive();
                s.addSOInvocation(soir);
            } catch (Exception e) {
                System.err.println("WARNING, SOI Mcast receive failed: " + e);
            }
        }
    }
}

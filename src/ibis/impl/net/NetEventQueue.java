package ibis.ipl.impl.net;

import java.util.Vector;

public class NetEventQueue {

        Vector v = null;
        
        public NetEventQueue() {
                v = new Vector();
        }

        synchronized public void put(NetEvent event) {
                v.add(event);
                notifyAll();
        }

        synchronized public NetEvent get() throws InterruptedException {
                if (v.size() == 0) {
                        wait();
                }

                return (NetEvent)v.remove(0);
        }
        
}

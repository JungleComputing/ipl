package ibis.ipl.impl.net;

final class NetEventQueueListener extends Thread {
                
        private NetPort port = null;
        private boolean end = false;
        private NetEventQueue queue = null;
                
        public NetEventQueueListener(NetPort port, String name, NetEventQueue queue) {
                super("Event queue listener: "+name);
                setDaemon(true);

                this.port  = port;
                this.queue = queue;
        }

        public void run () {
                while (!end) {
                        try {
                                NetEvent event = queue.get();
                        } catch (InterruptedException e) {
                                end = true;
                                continue;
                        }
                }
                        
        }
                
        public void end() {
                end = true;
                this.interrupt();
        }
}
        





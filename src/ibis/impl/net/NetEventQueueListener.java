package ibis.ipl.impl.net;

final class NetEventQueueListener extends Thread {
                
        private NetEventQueueConsumer cons  = null;
        private boolean               end   = false;
        private NetEventQueue         queue = null;
                
        public NetEventQueueListener(NetEventQueueConsumer cons, String name, NetEventQueue queue) {
                super("Event queue listener: "+name);
                setDaemon(true);

                this.cons  = cons;
                this.queue = queue;
        }

        public void run () {
                while (!end) {
                        try {
                                NetEvent event = queue.get();
                                cons.event(event);
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
        





package ibis.impl.net;

/**
 * Provide a thread dedicated to extract and dispatch {@linkplain
 * NetEvent events} from an {@linkplain NetEventQueue event queue} to
 * an {@linkplain NetEventQueueConsumer event consumer}.
 *
 * Several listeners are allowed to be attached to the same
 * {@linkplain NetEventQueue event queue}. In that case, the
 * dispatching scheme of the events to the {@linkplain
 * NetEventQueueConsumer event consumers} is unspecified.
 */
final class NetEventQueueListener extends Thread {

        /**
         * Reference the associated {@linkplain NetEventQueueConsumer
         * event consumer}.
         */
        private NetEventQueueConsumer cons  = null;

        /**
         * Set to true to indicated that the end of the listener has
         * been requested.
         */
        private volatile boolean      end   = false;

        /**
         * Reference the {@linkplain NetEventQueue event queue} the
         * thread is listening to.
         */
        private NetEventQueue         queue = null;

        private String               _name  = null;

        /**
         * Construct the listener.
         *
         * @param cons the consumer.
         * @param name the name of the thread (mainly for debugging purpose).
         * @param queue the queue to listen to.
         */
        public NetEventQueueListener(NetEventQueueConsumer cons, String name, NetEventQueue queue) {
                super("Event queue listener: "+name);

                this.cons  = cons;
                this.queue = queue;

                _name = "Event queue listener: "+name;
        }

        /**
         * Provide the main thread body.
         */
        public void run () {
                while (!end) {
                        try {
                                NetEvent event = queue.get();
                                cons.event(event);
                        } catch (InterruptedIOException e) {
                                // end = true;
                                // continue;
                        }
                }

        }

        /**
         * Terminate the listening thread.
         */
        public void end() {
                end = true;
		queue.end();
        }
}


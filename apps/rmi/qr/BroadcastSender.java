class BroadcastSender extends Thread {

    private i_BroadcastObject dest;

    private Object data;

    private int broadcast;

    private int owner;

    private boolean filled = false;

    public synchronized void put(i_BroadcastObject dest, int broadcast,
            Object data, int owner) {

        while (filled) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        filled = true;

        this.dest = dest;
        this.broadcast = broadcast;
        this.data = data;
        this.owner = owner;

        notifyAll();

        // Maybe we need a yield to switch to the sender thread...
        // Last time I checked, it didn't help at all.
        //	Thread.yield();
    }

    private synchronized void send() {

        while (!filled) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            dest.send(broadcast, data, owner);
        } catch (Exception e) {
            // eek !
            System.err.println("BroadcastSender got " + e);
            System.exit(1);
        }
        filled = false;
        notifyAll();
    }

    public void run() {
        while (true)
            send();
    }
}


/* $Id$ */

class Monitor extends Thread {

    Work w;

    Monitor(Work w) {
        this.w = w;
    }

    public void run() {
        while (true) {
            w.printWorking();

            try {
                sleep(1000);
            } catch (Exception e) {
                // don't care.
            }
        }
    }
}


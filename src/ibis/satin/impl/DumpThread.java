package ibis.satin.impl;

class DumpThread extends Thread {

    private Satin satin;

    DumpThread(Satin satin) {
        super("Satin dump thread");
        this.satin = satin;
    }

    public void run() {
        satin.q.print(System.err);
        satin.onStack.print(System.err);
        satin.outstandingJobs.print(System.err);
        satin.globalResultTable.print(System.err);
    }
}


package ibis.ipl.impl.registry.central.client;

public class StatisticsSender extends Thread {
    
    private final CommunicationHandler communicationHandler;
    
    private final long timeout;

    StatisticsSender(
            CommunicationHandler communicationHandler, long timeout) {
        this.communicationHandler = communicationHandler;
        
        this.timeout = timeout;
    }
    
    public synchronized void run() {
        communicationHandler.sendStatistics();
        
        
        //randomize delay (average still the given timeout)
        long delay = (long) (timeout * 2 * Math.random());
        
        try {
            if (delay > 0) {
                wait(delay);
            }
        } catch(InterruptedException e) {
            //IGNORE
        }
    }

}

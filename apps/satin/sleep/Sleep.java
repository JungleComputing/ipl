import java.util.Date;

/*
 * This is one of the most simple Satin programs possible. It sleeps :)
 */
final class Sleep extends ibis.satin.SatinObject implements SleepInterface {

    /**
     * Parallel Satin sleep
     * 
     */
    public double sleep(double seconds) {
        if (seconds <= 1.0) {
            System.err.println("sleeping " + seconds + " seconds");
            try {
                Thread.sleep((long) (seconds * 1000.0));
            } catch (InterruptedException e) {
                System.err.println("interrupred while sleeping: " + e);
            }
            return seconds;
        }

        double half = seconds / 2;

        double x = sleep(half);
        double y = sleep(half);
        sync();

        return x + y;
    }

    public static void main(String[] args) {
        double seconds = 30.0;

        if (args.length == 1) {
            seconds = Integer.parseInt(args[0]);
        } else if (args.length > 1) {
            System.out.println("Usage: Sleep <seconds>");
            System.exit(1);
        }

        Sleep sleep = new Sleep();

        System.out.println("Sleeping for " + seconds + " seconds from " + new Date() );

        long start = System.currentTimeMillis();
        double result = sleep.sleep(seconds);
        sleep.sync();
        double timeSatin = (double) (System.currentTimeMillis() - start) / 1000.0;

        System.out.println("application time sleep (" + seconds + ") took "
                + timeSatin + " s");
        
        System.out.println("result: " + result);

        
        System.out.println("speedup: " + seconds / timeSatin);

        System.out.println("Test succeeded!");
    }
}

/* $Id$ */

/*
 * This is one of the most simple Satin programs possible. It calculates
 * Fibonacci numbers. It calculates them in a very inefficient way, but this
 * program is used as an example and as a benchmark. It is not trying to be a
 * fast Fibonacci implementation.
 */
final class Fib extends ibis.satin.SatinObject implements FibInterface {
    /**
     * Sequential version, used for comparison
     * 
     * @param n
     * @return fib(n)
     */
    public long fibSeq(int n) {
        if (n < 2)
            return n;

        return fibSeq(n - 1) + fibSeq(n - 2);
    }

    /**
     * Parallel Satin version
     * 
     * @param n
     * @return fib(n)
     */
    public long fib(int n) {
        if (n < 2)
            return n;

        long x = fib(n - 1);
        long y = fib(n - 2);
        sync();

        return x + y;
    }

    /**
     * Much more efficient iterative version
     * 
     * @param n
     * @return fib(n)
     */
    static long fibIter(int n) {
        long f0, f1, f2;
        int i;
        if (n < 2) {
            return n;
        }
        f0 = 0;
        f1 = 1;
        f2 = 1;
        for (i = 2; i <= n; ++i) {
            f2 = f0 + f1;
            f0 = f1;
            f1 = f2;
        }

        return f2;
    }

    public static void main(String[] args) {
        long resSeq, resSatin;
        int n = 0;

        if (args.length == 0) {
            n = 28;
        } else if (args.length > 1) {
            System.out.println("Usage: fib <n>");
            System.exit(1);
        } else {
            n = Integer.parseInt(args[0]);
        }

        Fib f = new Fib();

        System.out.println("Running Fib " + n);

        long start = System.currentTimeMillis();
        resSeq = f.fibSeq(n);
        double timeSeq = (double) (System.currentTimeMillis() - start) / 1000.0;
        System.err.println("Sequential version took "  + timeSeq + " s");
        
        start = System.currentTimeMillis();
        resSatin = f.fib(n);
        f.sync();
        double timeSatin = (double) (System.currentTimeMillis() - start) / 1000.0;

        if (resSatin != resSeq) {
            System.out.println("application result fib GAVE WRONG RESULT! "
                    + resSatin + " should be " + resSeq);
            System.out.println("Test failed!");
            System.exit(1);
        } else {
            System.out.println("application time fib (" + n + ") took "
                    + timeSatin + " s");
            System.out.println("application result fib (" + n + ") = "
                    + resSatin);
            
            System.out.println("Satin overhead factor is: " + (timeSatin / timeSeq));
            System.out.println("Test succeeded!");
        }
    }
}
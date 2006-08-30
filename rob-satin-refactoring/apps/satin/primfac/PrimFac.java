/* $Id$ */

public final class PrimFac extends ibis.satin.SatinObject implements
        PrimFacInterface, java.io.Serializable {
    static final int THRESHOLD = 10000;

    static boolean is_prime(long n) {
        long i, sqrn;

        if (n == 2) {
            return true;
        } else if ((n % 2) == 0) {
            return false;
        } else {
            sqrn = (long) Math.sqrt((double) n) + 1;
            for (i = 3; i <= sqrn; i += 2) {
                if ((n % i) == 0) {
                    return false;
                }
            }
            return true;
        }
    }

    public String spawn_primfac(long n, long mini, long maxi) {
        return primfac(n, mini, maxi);
    }

    public String primfac(long n, long mini, long maxi) {
        long i, mid;
        String result = "";

        if (maxi - mini < PrimFac.THRESHOLD) {
            for (i = mini; i <= maxi; i++) {
                if ((n % i) == 0) {
                    if (is_prime(i)) {
                        long nn = n;
                        int f = 0;
                        while ((nn % i) == 0) {
                            nn /= i;
                            f++;
                        }
                        result = result + "|" + f + " times factor " + i;
                    }
                }
            }
        } else {
            mid = (maxi - mini) / 2;
            String result1 = spawn_primfac(n, mini, mini + mid);
            String result2 = spawn_primfac(n, mini + mid + 1, maxi);
            sync();
            result = result1 + result2;
        }
        return result;
    }

    public static void main(String[] args) {
        long N = 19678904;
        PrimFac p = new PrimFac();

        if (args.length == 0) {
        } else if (args.length == 1) {
            N = Long.parseLong(args[0]);
        } else {
            System.out.println("Usage: primfac <n>");
            System.exit(-6);
        }

        System.out.println("primfac(" + N + ") started");

        long start = System.currentTimeMillis();
        String result = p.spawn_primfac(N, 2, N);
        p.sync();
        long end = System.currentTimeMillis();
        double time = (end - start) / 1000.0;

        System.out.print("application result (" + N + ") = " + result);
        System.out.println("\napplication time primfac (" + N + ") took "
                + time + " s");
        if (args.length == 0) {
            if (!result
                    .equals("|3 times factor 2|1 times factor 7|1 times factor 127|1 times factor 2767")) {
                System.out.println("Test failed!");
                System.exit(1);
            } else {
                System.out.println("Test succeeded!");
            }
        }
    }
}
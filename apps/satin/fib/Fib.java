final class Fib extends ibis.satin.SatinObject implements FibInterface, java.io.Serializable  {

	int threshold;

	public Fib(int threshold) {
	    this.threshold = threshold;
	}

	public long fib_seq(int n) {
	    if (n < 2) return n;
	    return fib_seq(n-1) + fib_seq(n-2);
	}

	public long fib(int n) {
		long x, y;

//		System.err.println("running fib " + n);

		if(n < 2) return n;

		if (n < threshold) {
		    return fib_seq(n);
		}

		x = fib(n-1);
		y = fib(n-2);
		sync();
		return x + y;
	}

	static long fib_iter(int n) {
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
		long res, res2;
		int n = 0;

		if (args.length == 0) {
		    n = 30;
		} else if (args.length > 2) {
			System.out.println("Usage: fib <n> [ <threshold> ]");
			System.exit(1);
		} else {
		    n = Integer.parseInt(args[0]);
		}
		int threshold = 0;

		if (args.length == 2) {
		    threshold = Integer.parseInt(args[1]);
		}

		Fib f = new Fib(threshold);

		System.out.println("Running Fib " + n +", threshold = " + threshold);
		res2 = fib_iter(n);

		long start = System.currentTimeMillis();
		res = f.fib(n);
		f.sync();
		double time = (double) (System.currentTimeMillis() - start) / 1000.0;

		if(res != res2) {
			System.out.println("application result fib GAVE WRONG RESULT! " + res + " should be " + res2);
			if (args.length == 0) {
			    System.out.println("Test failed!");
			    System.exit(1);
			}
		} else {
			System.out.println("application time fib (" + n + ") took " + time + 
					   " s");
			System.out.println("application result fib (" + n + ") = " + res);
			if (args.length == 0) {
			    System.out.println("Test succeeded!");
			}
		}
	}
}

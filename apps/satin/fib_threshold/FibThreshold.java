final class FibThreshold extends ibis.satin.SatinObject implements FibThresholdInterface, java.io.Serializable  {

	static final long THRESHOLD = 32;

	public long spawn_fib(long n) {
		return fib(n);
	}

	long fib(long n) {
		long x, y;
		if(n<2) return n;

		if(n<THRESHOLD) {
			return fib(n-1) + fib(n-2);
		}

		x = spawn_fib(n-1);
		y = spawn_fib(n-2);
		sync();

		return x + y;
	}

	static long fib_iter(long n) {
		long f0, f1, f2, i;
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
		long n = Long.parseLong(args[0]);

		FibThreshold f = new FibThreshold();

		System.out.println("Running FibThreshold " + n);
		res2 = fib_iter(n);

		long start = System.currentTimeMillis();
		res = f.spawn_fib(n);
		f.sync();
		double time = (double) (System.currentTimeMillis() - start) / 1000.0;

		if(res != res2) {
			System.err.println("application fib_threshold GAVE WRONG RESULT! " + res2 + " should be " + res);
			System.exit(1);
		} else {
			System.out.println("application fib_threshold (" + n + ") took " + time + 
					   " seconds, result = " + res);
		}
	}
}

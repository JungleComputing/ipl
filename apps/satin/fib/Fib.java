final class Fib extends ibis.satin.SatinObject implements FibInterface, java.io.Serializable  {

	public long fib(long n) {
		long x, y;

//		System.err.println("running fib " + n);

		if(n < 2) return n;

		x = fib(n-1);
		y = fib(n-2);
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

		if(args.length != 1) {
			System.err.println("application fib got wrong number of parameters, need one");
			System.exit(1);
		}
		long n = Long.parseLong(args[0]);

		Fib f = new Fib();

		System.out.println("Running Fib " + n);
		res2 = fib_iter(n);

		long start = System.currentTimeMillis();
		res = f.fib(n);
		f.sync();
		double time = (double) (System.currentTimeMillis() - start) / 1000.0;

		if(res != res2) {
			System.err.println("application fib GAVE WRONG RESULT! " + res + " should be " + res2);
		} else {
			System.out.println("application fib (" + n + ") took " + time + 
					   " seconds, result = " + res);
		}
	}
}

public final class NoverK extends ibis.satin.SatinObject implements NoverKInterface, java.io.Serializable  {

	static final long THRESHOLD = 25;

	public long spawn_nok (long n, long k) {
		return nok(n, k);
	}

	public long nok (long n, long k) {
		long r1, r2;

		if ( n == k || k == 0) return 1;
		if ( k == 1 ) return n;

		if(n <= THRESHOLD) {
			r1 = nok(n-1, k-1);
			r2 = nok(n-1, k);
			return r1+r2;
		}

		r1 = spawn_nok(n-1, k-1);
		r2 = spawn_nok(n-1, k);
		sync();
		return r1+r2;
	}


	public static void main(String[] args) {
		int N;
		long res;
		NoverK n = new NoverK();

		if(args.length != 1) {
			System.out.println("Usage: noverk <n>");
			System.exit(-6);
		}

		N = Integer.parseInt(args[0]);

		System.out.println("n_over_k(" + N + " " + (N/2) + ") started");

		long start = System.currentTimeMillis();
		res = n.nok(N, N/2);
		n.sync();
		long end = System.currentTimeMillis();
		double time = (end-start)/1000.0;

		System.out.println("application n_over_k (" + N + "," + (N/2) + 
				   ") took " + time + " seconds, result = " + res);
	}
}

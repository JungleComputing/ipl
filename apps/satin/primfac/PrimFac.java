public final class PrimFac extends ibis.satin.SatinObject implements PrimFacInterface, java.io.Serializable  {
	static final int THRESHOLD = 10000;

	static boolean is_prime(long n) {
		long i, sqrn;
		
		if (n == 2) {
			return true;
		} else if ((n % 2) == 0) {
			return false;
		} else {
			sqrn = (long) Math.sqrt((double)n) + 1;
			for (i=3; i<=sqrn; i+=2 ) {
				if ( (n % i) == 0 ) {
					return false;
				}
			}
			return true;
		}
	}


	public void spawn_primfac(long n, long mini, long maxi) {
		primfac(n, mini, maxi);
	}

	public void primfac(long n, long mini, long maxi) {
		long i, mid;

		if ( maxi - mini < PrimFac.THRESHOLD ) {
			for (i=mini; i <= maxi; i++) {
				if ( (n % i) == 0 ) {
					if ( is_prime(i) ) {
						long nn = n; int f = 0;
						while ( (nn % i) == 0 ) {
							nn /= i;
							f++;
						}
						System.out.println("application result |" + f + " times factor " + i);
					}
				}
			}
		} else {
			mid = (maxi - mini) / 2;
			spawn_primfac(n,mini,mini+mid);
			spawn_primfac(n,mini+mid+1,maxi);
			sync();
		}
	}


	public static void main(String[] args) {
		long N;
		PrimFac p = new PrimFac();

		if(args.length != 1) {
			System.out.println("Usage: primfac <n>");
			System.exit(-6);
		}

		N = Long.parseLong(args[0]);

		System.out.println("primfac(" + N + ") started");

		System.out.print("application result (" + N + 
				   ") = ");

		long start = System.currentTimeMillis();
		p.spawn_primfac(N, 2, N);
		p.sync();
		long end = System.currentTimeMillis();
		double time = (end-start)/1000.0;

		System.out.println("\napplication time primfac (" + N + 
				   ") took " + time + " s");
	}
}

public class Main {
	public static final int LEN = 100*1024;
	public static final int COUNT = 10000;
	public static final int TESTS = 10;
		
	public static void main(String args[]) {

		long start, end;

		int[] a = new int[LEN/4];
		int[] b = new int[LEN/4];
		
		for (int j=0;j<TESTS;j++) { 
			
			start = System.currentTimeMillis();
			
			for (int i=0;i<COUNT;i++) {
				System.arraycopy(a, 0, b, 0, LEN/4);
			}
				
			end = System.currentTimeMillis();
			
			long time = end-start;
			double kb = COUNT*LEN;
			double ktp = ((1000.0*kb)/(1024*1024))/time;
				
			System.out.println("-new throughput = " + ktp + " MBytes/s");
		}

		for (int j=0;j<TESTS;j++) { 
			
			start = System.currentTimeMillis();
			
			for (int i=0;i<COUNT;i++) {
				int [] temp = new int[LEN/4];
				System.arraycopy(a, 0, temp, 0, LEN/4);
			}
				
			end = System.currentTimeMillis();
			
			long time = end-start;
			double kb = COUNT*LEN;
			double ktp = ((1000.0*kb)/(1024*1024))/time;
				
			System.out.println("+new throughput = " + ktp + " MBytes/s");
		}
	}
}

/* final */ class Lowlevel extends ibis.satin.SatinObject implements LowlevelInterface, java.io.Serializable  {

	public void spawn_void() {
		return;
	}

	public int spawn_int(int i) {
		return i;
	}

	public Object spawn_Object(Object o) {
		return o;
	}

	public void call_void() {
		return;
	}

	public int call_int(int i) {
		return i;
	}

	public Object call_Object(Object o) {
		return o;
	}

	public static void main(String[] args) {
		long start, end;
		double time;
		Object o = new Object();

		if(args.length != 1) {
			System.err.println("application lowlevel got wrong number of parameters, need one");
		}
		int n = Integer.parseInt(args[0]);

		Lowlevel l = new Lowlevel();

		System.out.println("Running Lowlevel " + n);

		for(int iter=0; iter<3; iter++) {
			System.out.println("---iter " + iter + "---"); 
			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				l.call_void();
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("call void took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");
			
			
			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				int res = l.call_int(0);
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("call int took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");


			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				Object res = l.call_Object(o);
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("call Object took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");


			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				l.spawn_void();
				l.sync();
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("spawn sync void took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");


			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				int res = l.spawn_int(0);
				l.sync();
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("spawn sync int took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");


			start = System.currentTimeMillis();
			for(int i=0; i<n; i++) {
				Object res = l.spawn_Object(o);
				l.sync();
			}
			end = System.currentTimeMillis();
			time = (double)(end - start);
			
			System.out.println("spawn sync Object took " + (time / 1000.0) + " seconds, " +
					   ((time * 1000.0) / n) + " micro's per call");
		}
	}
}

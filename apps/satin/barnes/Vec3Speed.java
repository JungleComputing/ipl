/**
 * This class is used for testing the speed of using Vec3 objects compared
 * to not using them
 * 
 * 14/11/03: with the IBM 1.4 JiT: 'Vec3' is 6 % slower than 'normal',
 *           on a das2 node        'Optimized' is 22 % slower than 'normal'
 */

strictfp class Vec3Speed {
	final static int ITERS = 4000000;
	
	Vec3 vec3Iteration() {
		Vec3 v = new Vec3(3.0, 5.0, 7.0);
		Vec3 w = new Vec3(3.14, 5.17, 1.234);

		v.mul(2.0);
		v.sub(w);
		v.div(4.0);
		v.add(w);

		return v;
	}

	Vec3 normalIteration() {
		Vec3 v = new Vec3(3.0, 5.0, 7.0);
		Vec3 w = new Vec3(3.14, 5.17, 1.234);

		v.x *= 2.0;
		v.y *= 2.0;
		v.z *= 2.0;

		v.x -= w.x;
		v.y -= w.y;
		v.z -= w.z;

		v.x /= 4.0;
		v.y /= 4.0;
		v.z /= 4.0;

		v.x += w.x;
		v.y += w.y;
		v.z += w.z;

		return v;
	}

	Vec3 optimIteration() {
		Vec3 v = new Vec3(3.0, 5.0, 7.0);
		Vec3 w = new Vec3(3.14, 5.17, 1.234);

		v.x = (v.x * 2.0 - w.x) / 4.0 + w.x;
		v.y = (v.y * 2.0 - w.y) / 4.0 + w.y;
		v.z = (v.z * 2.0 - w.z) / 4.0 + w.z;

		return v;
	}

	public long[] measure() {
		int i;
		Vec3 v0 = new Vec3(), v1 = new Vec3(), v2 = new Vec3();
		long start[] = new long[3], end[] = new long[3];
		
		start[0] = System.currentTimeMillis();
		for (i = 0; i < ITERS; i++) {
			v0.add(vec3Iteration());
		}
		end[0] = System.currentTimeMillis();

		start[1] = System.currentTimeMillis();
		for (i = 0; i <ITERS; i++) {
			Vec3 w = normalIteration();
			v1.x += w.x;
			v1.y += w.y;
			v1.z += w.z;
		}
		end[1] = System.currentTimeMillis();

		start[2] = System.currentTimeMillis();
		for (i = 0; i < ITERS; i++) {
			Vec3 w = optimIteration();
			v2.x += w.x;
			v2.y += w.y;
			v2.z += w.z;
		}
		end[2] = System.currentTimeMillis();

		if (!v0.equals(v1) || !v0.equals(v2)) {
			System.err.println("EEK: incorrect vectors!");
			System.exit(1);
		}

		end[0] -= start[0];
		end[1] -= start[1];
		end[2] -= start[2];
		return end;
	}

	void start() {
		int i;
		long[][] mm = new long[3][]; //MeasureMents
		long[] avg = new long[3];

		//warm up the JiT four times, then do the 'real' measurement
		for(i = 0; i < 5; i++) {
			mm[0] = measure();	
			mm[1] = measure();
			mm[2] = measure();
		}

		for (i = 0; i < 3; i ++) {
			avg[i] = (mm[0][i] + mm[1][i] + mm[2][i]) / 3;
		}
			
		System.out.println("type       t0   t1   t2  avg");
		System.out.println("Vec3:     " + mm[0][0] + " " + mm[1][0] + " " +
						   mm[2][0] + " " + avg[0]);
		System.out.println("normal:   " + mm[0][1] + " " + mm[1][1] + " " +
						   mm[2][1] + " " + avg[1]);
		System.out.println("optimized:" + mm[0][2] + " " + mm[1][2] + " " +
						   mm[2][2] + " " + avg[2]);

	}
	public static void main(String args[]) {
		new Vec3Speed().start();
	}
}

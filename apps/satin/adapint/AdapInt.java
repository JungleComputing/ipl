final class AdapInt extends ibis.satin.SatinObject implements AdapIntInterface, java.io.Serializable  {

    static final double EPSILON = 0.0001;
    static final double THRESHOLD = 10000000.0 * EPSILON;

    static double f(double x) {
	return Math.sin(x)*0.1*x;
    }

    public double spawn_integrate(double a, double b, double epsilon) {
	return integrate(a, b, epsilon);
    }

    public double integrate(double a, double b, double epsilon) {
	double total, left, right, diff;
	double delta, deltahalf, mid;
	double fa, fb, fmid;
	double res1, res2;

	delta = (b - a) / 2.0;
	deltahalf = delta / 2.0;
	mid = delta + a;
	fa = f(a);
	fb = f(b);
	fmid = f(mid);
	total = delta * (fa + fb);
	left = deltahalf * (fa + fmid);
	right = deltahalf * (fb + fmid);
	diff = total - (left + right);
	if(diff < 0.0) diff = -diff;
	if(diff < epsilon) {
	    return total;
	} else if(diff > THRESHOLD) { 
	    res1 = spawn_integrate(mid, b, epsilon);
	    res2 = spawn_integrate(a, mid, epsilon);
	    sync();

	    return res1 + res2;
	} else {
	    return integrate(mid, b, epsilon) + integrate(a, mid, epsilon);
	}
    }



    public static void main(String[] args) {
	double a, b, epsilon;
	double res;
	AdapInt adap = new AdapInt();

	if (args.length == 0) {
	    a = 0.0;
	    b = 64000.0;
	    epsilon = 0.1;
	}
	else {
	    if (args.length != 3) {
		System.out.println("Usage: adapint <start> <end> <epsilon>");
		System.out.println("Try: adapint 0 400 0.0001");
		System.exit(-6);
	    }

	    a = Double.parseDouble(args[0]);
	    b = Double.parseDouble(args[1]);
	    epsilon = Double.parseDouble(args[2]);
	}

	System.out.println("Starting integrate, from " + a + " to " + b + " epsilon = " + epsilon);

	long start = System.currentTimeMillis();
	res = adap.integrate(a, b, epsilon);
	adap.sync();
	long end = System.currentTimeMillis();
	double time = (end - start) / 1000.0;

	System.out.println("application time integrate (" + (int)a + "," + (int)b + "," + epsilon + 
		") took " + time + " s");
	System.out.println("application result integrate (" + (int)a + "," + (int)b + "," + epsilon + 
		") result = " + res);
	if (args.length == 0) {
	    double diff = res - -5535.065343835229;

	    if (diff > 0.0001 || diff < -0.0001) {
		System.out.println("Result should be -5535.065343835229");
		System.out.println("Test failed!");
		System.exit(1);
	    }
	}
    }
}

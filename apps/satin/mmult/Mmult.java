//
// Class Mmult
//
// Matrix multiply functionality
// This is the ony really interesting part
// The rest is dead weight
//
final class Mmult extends ibis.satin.SatinObject implements MmultInterface, java.io.Serializable  {

    // real  functionality: tasked-mat-mul
    public Matrix mult(int task, int rec, Matrix a, Matrix b, Matrix c) {

	Matrix f_00=null;
	Matrix f_01;
	Matrix f_10;
	Matrix f_11;

	if (task == 0) {
	    // switch to serial recursive part
	    // pass instance variables
	    c.recMatMul(rec, a, b);
	    return c;
	}

	if (task > 0) {
	    task--;
	} else {
	    rec--;
	}

	f_00 = /* spawn */ mult(task, rec, a._00, b._00, c._00);
	f_01 = /* spawn */ mult(task, rec, a._00, b._01, c._01);
	f_10 = /* spawn */ mult(task, rec, a._10, b._00, c._10);
	f_11 = /* spawn */ mult(task, rec, a._10, b._01, c._11);
	sync();

	// spawn child threads; k = 1; after having synched on
	// get_result() sync on the child threads
	// it's slightly inefficient that 01 will be started after 00, a
	// completely unrelated sub matrix, is ready
	// what we really want is systolicism, where f_01 is started as
	// soon as its data dependency is resolved.  Now, the sematics
	// of join are that it blocks the entire parent thread, instead
	// of just the 00 call. Mentat has the data dependency
	// analysis that we want.

	c._00 = /* spawn */ mult(task, rec, a._01, b._10, f_00); 
	c._01 = /* spawn */ mult(task, rec, a._01, b._11, f_01);
	c._10 = /* spawn */ mult(task, rec, a._11, b._10, f_10);
	c._11 = /* spawn */ mult(task, rec, a._11, b._11, f_11);
	sync();

	return c;
    }


    public static int power(int base, int exponent) {
	return (int) Math.pow(base, exponent);
    }


    public static void main(String args[]) {
	int task=2, rec=2, loop=power(2, 2);
	long start, end;
	double time;
	Mmult m = new Mmult();

	if (args.length == 3) {
	    task = Integer.parseInt(args[0]);
	    rec  = Integer.parseInt(args[1]);
	    loop = Integer.parseInt(args[2]);
	} else if (args.length != 0) {
	    System.out.println("usage: mmult [task rec loop]");
	    System.exit(66);
	}

	int cells = power(2, task+rec) * loop;
	System.out.println("Running Matrix multiply, on a matrix of size " + cells + " x " + cells 
		+ ", threads = " + power(8, task));

	Matrix a = new Matrix(task, rec, loop, 1.0f, false); // a is row-wise flipped, to make product zero
	Matrix b = new Matrix(task, rec, loop, 1.0f, false);
	Matrix c = new Matrix(task, rec, loop, 0.0f, false);

	//    System.out.println("A:");
	//    a.print(task, rec, loop);
	//    System.out.println("\nB:");
	//    b.print(task, rec, loop);

	start = System.currentTimeMillis();
	c = /* spawn */ m.mult(task, rec, a, b, c);
	m.sync();
	end = System.currentTimeMillis();
	time = (double) end - start;
	time /= 1000.0; // seconds.

	System.out.println("checking result, should be " + ((float) cells));
	if(c.check(task, rec, (float) cells)) {
	    //    System.out.println("\nC:");
	    //    c.print(task, rec, loop);
	    System.out.println("application time Mmult (" + task + "," + rec + "," +
		    loop + "," + cells + ") took " + time + " s");
	    System.out.println("application result Mmult (" + task + "," + rec + "," +
		    loop + "," + cells + ") = OK");
	    System.out.println("Test succeeded!");
	} else {
	    System.out.println("application time Mmult (" + task + "," + rec + "," +
		    loop + "," + cells + ") GAVE WRONG RESULT!");
	    System.out.println("application result Mmult (" + task + "," + rec + "," +
		    loop + "," + cells + ") GAVE WRONG RESULT!");
	    System.out.println("Test failed!");
	    System.exit(1);
	}
    }
} 

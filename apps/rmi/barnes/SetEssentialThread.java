strictfp
public class SetEssentialThread extends Thread {

static final int METHOD_SERIALIZE = 0;
static final int METHOD_NORMAL = 1;


boolean filled = false;
Processor dest;
int src;
int bCount;
SendBody [] b;
int cCount;
CenterOfMass [] c;
double[] bp;
double[] cp;
int method;


SetEssentialThread(Processor dest) {
	super("SetEssentialThread");
	this.dest = dest;
	start();
}


public synchronized void put(int Source, int bCount, double [] bp, int cCount, double [] cp ) {
	while(filled) {
		try {
			wait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	filled = true;

	this.src = Source;
	this.bCount = bCount;
	this.bp = bp;
	this.cCount = cCount;
	this.cp = cp;
	this.method = METHOD_SERIALIZE;

	notifyAll();

	// Maybe we need a yield to switch to the sender thread...
	// Last time I checked, it didn't help at all.
	//	Thread.yield();
}


public synchronized void put(int Source, int bCount, SendBody [] b, int cCount, CenterOfMass [] c) {
	while(filled) {
		try {
			wait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	filled = true;

	this.src = Source;
	this.bCount = bCount;
	this.b = b;
	this.cCount = cCount;
	this.c = c;
	this.method = METHOD_NORMAL;

	notifyAll();

	// Maybe we need a yield to switch to the sender thread...
	// Last time I checked, it didn't help at all.
	//	Thread.yield();
}


public synchronized void send() {
	while(!filled) {
		try {
			wait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	try {
		switch (method) {
			case METHOD_SERIALIZE:
				dest.setEssential(src, bCount, bp, cCount, cp);
				break;
			case METHOD_NORMAL:
				dest.setEssential(src, bCount, b, cCount, c);
				break;
			default:
				System.out.println("Illegal method in send\n");
				new Exception().printStackTrace();
				System.exit(1);
		}
	} catch (Exception e) {
		System.out.println("eek, setEssential generated an exception...");
		e.printStackTrace();
	}

	filled = false;

	notifyAll();
}


public void run() {
	while(true) send();
}

}

public class Barrier
{
    private int goal;
    private int count;


    public Barrier(int goal) {
	count = 0;
	this.goal = goal;
    }


    public synchronized void sync() {
	count++;
	//System.out.println("counter to: " + count);
	if (count == goal) {
	    notifyAll();
	    count = 0;
	} else {
	    try {
		wait();
	    } catch (Exception e) {
		// jackal doesn't do this anyway...
		sync();
	    }
	}
    }
}

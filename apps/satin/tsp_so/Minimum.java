

final class Minimum extends ibis.satin.SharedObject
    implements java.io.Serializable,MinimumWriteMethodsInterface {

    int val = Integer.MAX_VALUE;

    /*write method*/
    public void set (int new_val) {
	if (new_val < val) {
            if (Tsp.verbose) {
                System.err.println("updating min to " + new_val);
            }	
	    val = new_val;
	}
    }
    
    /*read method*/
    public int get() {
	return val;
    }
    
    public Minimum() {};
}

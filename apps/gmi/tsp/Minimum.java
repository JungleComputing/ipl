import ibis.gmi.GroupMember;

public class Minimum extends GroupMember implements i_Minimum {

	private int minimum;
//	int updates = 0;
//	int sets = 0;
//	int gets = 0;
	
	public Minimum() {
		super();
		minimum = Integer.MAX_VALUE;	
	}

	public void set(int min) {
		synchronized (this) { 
	 	    if(min < this.minimum) {
			minimum = min;
//			updates++;			
		    }
                }
//		sets++;
	}

	public int get() {
//		gets++;
		return minimum;
	}
}

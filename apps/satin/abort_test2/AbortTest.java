/* $Id$ */

final class AbortTest extends ibis.satin.SatinObject implements AbortTestInterface, java.io.Serializable  {
	public void foo() throws Exception {
	    throw new Exception();
	}

	public static void main(String[] args) {
		AbortTest t = new AbortTest();

		for(int i=0; i<5000; i++) {
		    try {
			t.foo();
		    } catch (Exception e) {
			if(Math.random() * 1000 < 3) {
			    t.abort();
			}
		    }
		}
		t.sync();
	}
}

final class TwoOutThree extends ibis.satin.SatinObject implements TwoOutThreeInterface, java.io.Serializable  {
	public void foo(int i) throws Done {
		throw new Done(i);
	}

	public static void main(String[] args) {
		TwoOutThree t = new TwoOutThree();
		int n = 0;

		for(int i=0; i<3; i++) {
			try {
				t.foo(i);
			} catch (Done d) {
				System.out.println("foo res: " + d.res);
				if (++n == 2) {
					t.abort();
				}
			}
		}
		t.sync();
	}
}

final class TwoOutThree extends ibis.satin.SatinObject implements TwoOutThreeInterface, java.io.Serializable  {
	public void foo() throws Done {
		throw new Done();
	}

	public static void main(String[] args) {
		TwoOutThree t = new TwoOutThree();
		int n = 0;

		for(int i=0; i<3; i++) {
			try {
				t.foo();
			} catch (Done d) {
				System.out.println("in Catch, i = " + i + ", n is now " + n);
				if (++n == 2) {
					t.abort();
					return;
				}
			}
		}

		t.sync();
	}
}

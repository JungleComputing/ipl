final class TwoOutThree extends ibis.satin.SatinObject implements TwoOutThreeInterface, java.io.Serializable  {
	public void foo() throws Done {
		throw new Done();
	}

	public static void main(String[] args) {
		TwoOutThree t = new TwoOutThree();
		int n = 0;

		System.out.print("application result two_out_of_three = ");
		for(int i=0; i<3; i++) {
			try {
				t.foo();
			} catch (Done d) {
				System.out.print("| in Catch, n is now " + n);
				if (++n == 2) {
					t.abort();
				}
				return;
			}
		}

		t.sync();

		System.out.println();
	}
}

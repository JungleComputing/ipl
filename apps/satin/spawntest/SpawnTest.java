class SpawnTest {

	SpawnTest() {}

	void start() {
		Test t = new Test();
		int i;
		int[] x = new int[100];
		int[] y = new int[100];

		for (i = 0; i < 100; i++) {
			x[i] = t.spawn_test(0);
			y[i] = t.spawn_test(10);
			t.sync();
		}
		System.out.println("application result: x = " + x[50] +
						   " y = " + y[50]);
	}

	public static void main(String args[]) {
		new SpawnTest().start();
	}
}


class SpawnTest {

	SpawnTest() {}

	void start() {
		Test t = new Test();
		int i, x = 0, y = 0;
		for (i = 0; i < 100; i++) {
			x = t.spawn_test(0);
			y = t.spawn_test(10);
			t.sync();
		}
		System.out.println("application result: x = " + x + " y = " + y);
	}

	public static void main(String args[]) {
		new SpawnTest().start();
	}
}


class SpawnTest {

	SpawnTest() {}

	void start() {
		Test t = new Test();

		for (int i = 0; i < 100; i++) {
			t.spawn_test(0);
			//t.spawn_test(1);
		}
	}

	static void main(String args[]) {
		new SpawnTest().start();
		System.out.println("application result: Test done");
	}
}


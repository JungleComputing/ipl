class SpawnTest {

    SpawnTest() {}

    void start() {
	Test t = new Test();
	int x = 0, y = 0;

	x = t.spawn100(0);
	y = t.spawn100(10);

	System.out.println("application result: x = " + x + " y = " + y);
    }

    public static void main(String args[]) {
	new SpawnTest().start();
    }
}


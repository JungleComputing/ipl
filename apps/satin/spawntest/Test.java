class Test extends ibis.satin.SatinObject implements TestInterface {

	public Test() {}

	public void spawn_test(int depth) {
		if (depth <= 0) return;
		spawn_test(depth - 1);
	}
}

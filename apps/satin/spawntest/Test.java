class Test extends ibis.satin.SatinObject implements TestInterface {

	public Test() {}

	public int spawn_test(int depth) {
		if (depth <= 0) return 0;
		int res = spawn_test(depth - 1);
		sync();
		return 1 + res;
	}
}

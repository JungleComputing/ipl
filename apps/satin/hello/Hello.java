final class Hello extends ibis.satin.SatinObject implements HelloInterface, java.io.Serializable  {

	int x;

	public int hello(int n) {
		return n;
	}

	public static void main(String[] args) {
		Hello h = new Hello();

		h.x = h.hello(1);
		h.sync();
	}
}

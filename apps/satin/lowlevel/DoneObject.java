final class DoneObject extends Exception {
	Object o;

	DoneObject(Object o) {
		this.o = o;
	}
}

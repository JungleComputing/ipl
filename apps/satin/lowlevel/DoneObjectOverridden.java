final class DoneObjectOverridden extends Exception {
	Object o;

	DoneObjectOverridden(Object o) {
		this.o = o;
	}

	public Throwable fillInStackTrace() {
		return this;
	}
}

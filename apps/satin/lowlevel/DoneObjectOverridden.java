final class DoneObjectOverridden extends Throwable {
	Object o;

	DoneObjectOverridden(Object o) {
		this.o = o;
	}

	public Throwable fillInStackTrace() {
		return null;
	}
}

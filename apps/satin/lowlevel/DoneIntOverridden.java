final class DoneIntOverridden extends Exception {
	int i;

	DoneIntOverridden(int i) {
		this.i = i;
	}

	public Throwable fillInStackTrace() {
		return this;
	}
}

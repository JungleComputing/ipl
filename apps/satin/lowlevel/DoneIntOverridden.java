final class DoneIntOverridden extends Throwable {
	int i;

	DoneIntOverridden(int i) {
		this.i = i;
	}

	public Throwable fillInStackTrace() {
		return null;
	}
}

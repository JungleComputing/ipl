final class DoneVoidOverridden extends Exception {
	public Throwable fillInStackTrace() {
		return this;
	}
}

final class DoneObjectOverridden extends Throwable {
	SerializableObject o;

	DoneObjectOverridden(SerializableObject o) {
		this.o = o;
	}

	public Throwable fillInStackTrace() {
		return null;
	}
}

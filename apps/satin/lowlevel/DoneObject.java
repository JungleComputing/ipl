final class DoneObject extends Throwable {
	SerializableObject o;

	DoneObject(SerializableObject o) {
		this.o = o;
	}
}

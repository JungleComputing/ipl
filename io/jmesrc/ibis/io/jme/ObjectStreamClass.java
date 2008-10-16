package ibis.io.jme;

public class ObjectStreamClass {
	private Class type;
	
	public ObjectStreamClass(Class type) {
		this.type = type;
	}
	
	public Class forClass() {
		return type;
	}
}

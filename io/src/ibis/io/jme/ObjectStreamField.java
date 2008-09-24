package ibis.io.jme;

/*
 * TODO: Need to figure out how to support this.
 */
public class ObjectStreamField {
	private String name;
	private Class type;
	private boolean unshared = false;
	
	public ObjectStreamField(String name, Class type) {
		this.name = name;
		this.type = type;
	}
	public ObjectStreamField(String name, Class type, boolean unshared) {
		this.name = name;
		this.type = type;
		this.unshared = unshared;
	}
	
}

package ibis.ipl.impl.mx;

public class Identifier implements Identifiable<Identifier> {
	IdManager<Identifier> manager;
	short id;

	public IdManager<Identifier> getIdManager() {
		return manager;
	}

	public short getIdentifier() {
		return id;
	}

	public void setIdManager(IdManager<Identifier> manager) {
		this.manager = manager;
	}

	public void setIdentifier(short id) {
		this.id = id;
	}



}

package ibis.ipl.impl.mx;

interface Identifiable<T extends Identifiable<T>> {
	void setIdManager(IdManager<T> manager);
	IdManager<T> getIdManager();
	short getIdentifier();
	void setIdentifier(short id);
}

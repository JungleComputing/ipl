package ibis.ipl.impl.mx;

class MxId<T> implements Comparable<MxId<T>> {
	short value;
	IdManager<T> myManager;
	T owner;
	
	MxId(IdManager<T> m) {
		value = 0;
		myManager = m;
	}

	public int compareTo(MxId<T> other) {
		return value - other.value;
	}
	
	public void remove() {
		myManager.free(this);
	}
	
}

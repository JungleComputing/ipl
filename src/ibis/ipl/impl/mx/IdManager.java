package ibis.ipl.impl.mx;

import java.util.Iterator;
import java.util.TreeMap;

// ID 0 will be kept free
class IdManager<T extends Identifiable<T>> {

	private TreeMap<Short, T> map;
	short last;
	
	IdManager () {
		map = new TreeMap<Short, T>();
		last = 0;
	}

	synchronized short insert(T t) throws Exception {
		short id;
		int size = map.size();
		if (size >= Short.MAX_VALUE) {
			// no ID left
			throw new Exception("ID Manager is out of ID's");
			//return 0;
		}

		//find an id:
		if (size == 0) {
			// set is empty, this is the first element
			id = 1;
			map.put(Short.valueOf(id), t);
			t.setIdentifier(id);
			t.setIdManager(this);
			return id;
		}
		
		// try to add t at the low end
		id = (short)(map.firstKey().shortValue() - 1);
		if (id > 0) {
			map.put(Short.valueOf(id), t);
			t.setIdentifier(id);
			t.setIdManager(this);
			return id;
		}

		// try the id that was freed recently
		if (last > 0) {
			id = last;
			if (!map.containsKey(Short.valueOf(id))) {
				map.put(Short.valueOf(id), t);
				// modify the supporting variables
				last = 0;
				t.setIdentifier(id);
				t.setIdManager(this);
				return id;
			}
		}

		// try to add t at the high end
		id = (short)(map.lastKey().shortValue() + 1);
		if (id <= Short.MAX_VALUE) {
			map.put(Short.valueOf(id), t);
			t.setIdentifier(id);
			t.setIdManager(this);
			return id;
		}
		// well, we really have to iterate over all the ids...
		Iterator<Short> iter = map.keySet().iterator();
		short key;
		id =  1;
		while(iter.hasNext()) {
			key = iter.next();
			if(id < key) {
				//found!
				map.put(Short.valueOf(id), t);
				t.setIdentifier(id);
				t.setIdManager(this);
				return id;
			}
			id = ++key;
		}
				
		// we didn't succeed...
		throw new Exception("ID Manager could not find an ID");
		//return 0;
	}

	
	synchronized void remove(short key) {
		T removedItem = map.remove(key);
		if (removedItem == null) {
			// key was not present in map
			//TODO maybe throw an exception here?
			return;
		} else {
			// success
			last = key;
			removedItem.setIdManager(null);
			removedItem.setIdentifier((short)0);
			return;
		}
	}
	
	synchronized int size() {
		return map.size();
	}

	synchronized T find(short id) {
		return map.get(id);
	}
}

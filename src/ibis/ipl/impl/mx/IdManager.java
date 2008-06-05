package ibis.ipl.impl.mx;

import java.util.TreeSet;

// ID 0 will be kept free
class IdManager<T> {

	private TreeSet<MxId<T>> set;
	short min, max, last;
	
	IdManager () {
		set = new TreeSet<MxId<T>>();
		min = max = 0;
		last = 0;
	}

	MxId<T> get() {
		MxId<T> id = new MxId<T>(this);
		
		synchronized(this) {
			int size = set.size();
			if(size >= Short.MAX_VALUE) {
				// no ID left
				return null;				
			}
					
			//find an id:
			if(size == 0) {
				// set is empty, this is the first element
				min = max = id.value = 1;
				if(set.add(id) == true) {
					return id;
				} else {
					// not possible?
					min = max = 0;
					return null;
				}
			}
			if(min > 1) {
				// add at the low end
				id.value = --min;
				if(set.add(id) == true) {
					// success
					return id;
				} else {
					// undo the changes to min
					min++;
				}
			}
			if(last > 0) {
				// try the id that was freed recently
				id.value = last;
				if(set.add(id) == true) {
					// success!
					if (last < min) {
						min = last;
					}
					if (last > max) {
						max = last;
					}
					last = 0;
					return id;
				}
			}
			if(max > 1) {
				// add at the high end
				id.value = ++max;
				if(set.add(id) == true) {
					// success
					max++;
					return id;
				} else {
					// undo the changes to max
					max++;
				}
			}
			// we didn't succeed...
			return null;
		}
	}
	
	synchronized boolean free(MxId<T> id) {
		if(id.myManager == this) {
			if(set.remove(id)) {
				// success
				last = id.value;
				id.myManager = null;
				return true;
			} else {
				//error, should not be possible to happen
				return false;
			}
		} else {
			// id is not managed by me, so do nothing
			return false;
		}
	}
	
	synchronized int size() {
		return set.size();
	}
}

package ibis.ipl.impl.net;

import java.util.HashMap;
import java.util.Random;

public final class NetBank {
	HashMap map = null;
	Random  rnd = null;
	
	public NetBank() {
		map = new HashMap();
		rnd = new Random();
	}

	public synchronized Long getUniqueKey() {
		Long key = null;
		do {
			key = new Long(rnd.nextLong());
		} while (map.containsKey(key));

		map.put(key, null);

		return key;
	}
	
	public synchronized void put(Long key, Object obj) {
		if (!map.containsKey(key))
			__.abort__("invalid key");

		map.put(key, obj);
	}

	public synchronized Object get(Long key) {
		if (!map.containsKey(key))
			__.abort__("invalid key");

		return map.get(key);
	}

	public synchronized Object exchange(Long key, Object obj) {
		if (!map.containsKey(key))
			__.abort__("invalid key");

		Object previousObj = map.get(key);
		map.put(key, obj);

		return previousObj;
	}

	public synchronized Object discardKey(Long key) {
		if (!map.containsKey(key))
			__.abort__("invalid key");

		return map.remove(key);
	}
	
}

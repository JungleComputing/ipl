import ibis.group.GroupMember;

class Test extends GroupMember implements myGroup { 

	public final static boolean DEBUG = false;

	int i;
	Object data;

	Test() { 
		if (DEBUG) System.out.println(rank + ": Test()");
	} 

	public void groupInit() { 
		i = rank;
		if (DEBUG) System.out.println(rank + ": Test.groupInit()");
	}

	public void put(Object o) { 
		if (DEBUG) System.out.println(rank + ": Test.put()");
		data = o;
	} 

	public Object get() { 
		if (DEBUG) System.out.println(rank + ": Test.get()");
		return data;
	} 

	public Object put_get(Object o) { 
		if (DEBUG) System.out.println(rank + ": Test.put_get()");
		return o;
	} 
} 

import ibis.group.GroupMember;

class Test extends GroupMember implements myGroup { 

	public final static boolean DEBUG = false;

	int i;

	Test() { 
		if (DEBUG) System.out.println(rank + ": Test()");
	} 

	public void groupInit() { 
		i = rank;
		if (DEBUG) System.out.println(rank + ": Test.groupInit()");
	}

	public void put(int i) { 
		if (DEBUG) System.out.println(rank + ": Test.put(" + i + ")");
		this.i = i;
	} 

	public int get() { 
		if (DEBUG) System.out.println(rank + ": Test.get() = " + i);
		return i;
	} 
} 

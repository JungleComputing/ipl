import ibis.gmi.GroupMember;

class Test extends GroupMember implements myGroup { 

	public final static boolean DEBUG = false;

	int i;

	Test() { 
		if (DEBUG) System.out.println(myGroupRank + ": Test()");
	} 

	public void groupInit() { 
		i = myGroupRank;
		if (DEBUG) System.out.println(myGroupRank + ": Test.groupInit()");
	}

	public void put(int i) { 
		if (DEBUG) System.out.println(myGroupRank + ": Test.put(" + i + ")");
		this.i = i;
	} 

	public int get() { 
		if (DEBUG) System.out.println(myGroupRank + ": Test.get() = " + i);
		return i;
	} 
} 

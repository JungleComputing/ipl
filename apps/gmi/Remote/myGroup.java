import ibis.group.GroupMethods;

interface myGroup extends GroupMethods { 

	public void put(Object o);
	public Object get();
	public Object put_get(Object o);
} 

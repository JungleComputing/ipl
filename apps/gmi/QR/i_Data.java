import ibis.group.GroupInterface;

interface i_Data extends GroupInterface { 
	public void put(int cpu, int time);
}

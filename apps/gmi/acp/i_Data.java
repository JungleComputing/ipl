import ibis.gmi.GroupInterface;

interface i_Data extends GroupInterface { 

    public void put(int cpu, int removed, 
	    long checks, long time, 
	    int modif, int change_ops, int revise);

    public String result();
}

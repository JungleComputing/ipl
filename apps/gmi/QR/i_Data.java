import ibis.gmi.GroupInterface;

interface i_Data extends GroupInterface { 
    public void put(int cpu, int time);
}

import ibis.gmi.GroupInterface;

interface i_Work extends GroupInterface { 

    public void vote(int var, boolean vote);
    public void announce(int var);
    public void ready(int cpu);
    public boolean workFor(int cpu);
    public boolean [] getWork(int cpu);
}

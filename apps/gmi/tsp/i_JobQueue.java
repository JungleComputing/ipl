import ibis.group.GroupInterface;

interface i_JobQueue extends GroupInterface {
	public Job getJob();
	public DistanceTable getTable();
	public void barrier();
}

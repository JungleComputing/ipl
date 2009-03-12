package ibis.ipl.registry.central.client;

public interface RegistryMBean {
	
	public String getPoolName();

	public int getPoolSize();

	public boolean getTerminated();

	public boolean getClosed();
	
	public String getTime();
	
}

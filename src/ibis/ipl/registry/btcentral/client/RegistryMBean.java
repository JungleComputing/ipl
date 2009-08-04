package ibis.ipl.registry.btcentral.client;

public interface RegistryMBean {
	
	public String getPoolName();

	public int getPoolSize();

	public boolean getTerminated();

	public boolean getClosed();
	
	public String getTime();
	
}

package ibis.ipl;

public interface ReceivePortIdentifier { 
	public boolean equals(ReceivePortIdentifier other);
	//gosia
	public int hashCode();
	//end gosia
	public String type();	
	public String name();	
	public IbisIdentifier ibis();
} 



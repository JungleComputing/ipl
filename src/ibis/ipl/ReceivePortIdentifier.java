package ibis.ipl;

public interface ReceivePortIdentifier { 
	public boolean equals(ReceivePortIdentifier other);
	public String type();	
	public String name();	
	public IbisIdentifier ibis();
} 



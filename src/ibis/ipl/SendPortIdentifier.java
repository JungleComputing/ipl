package ibis.ipl;

public interface SendPortIdentifier { 
	public boolean equals(SendPortIdentifier other);
	public String type();
	public String name();
	public IbisIdentifier ibis();
} 

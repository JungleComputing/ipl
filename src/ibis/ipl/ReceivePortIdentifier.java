package ibis.ipl;

public interface ReceivePortIdentifier { 
	public String type();	
	public String name();	
	public IbisIdentifier ibis();

	/** The hashCode method is mentioned here just as a reminder that an implementation
	    must probably redefine it, because two objects representing the same
	    ReceivePortIdentifier must result in the same hashcode (and compare equal).
	    To explicitly specify it in the interface does not help, because java.lang.Object
	    already implements it, but, anyway, here it is:
	**/
	public int hashCode();

	/** The equals method is mentioned here just as a reminder that an implementation
	    must probably redefine it, because two objects representing the same
	    ReceivePortIdentifier must compare equal (and result in the same hashcode).
	    To explicitly specify it in the interface does not help, because java.lang.Object
	    already implements it, but, anyway, here it is:
	**/
	public boolean equals(Object other);
} 



package ibis.io;

public interface Serializable { 
	public void generated_WriteObject(MantaOutputStream out) throws ibis.ipl.IbisIOException;
//	public void generated_ReadObject(MantaInputStream in) throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException;
} 

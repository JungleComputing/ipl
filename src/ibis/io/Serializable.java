package ibis.io;

public interface Serializable { 
	public void generated_WriteObject(MantaOutputStream out) throws java.io.IOException;
	public void generated_ReadObject(MantaInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException;
} 

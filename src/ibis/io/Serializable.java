package ibis.io;

import java.io.IOException;

public interface Serializable { 
	public void generated_WriteObject(IbisSerializationOutputStream out)
		throws IOException;
	public void generated_DefaultWriteObject(IbisSerializationOutputStream out,
											 int lvl) throws IOException;
	public void generated_DefaultReadObject(IbisSerializationInputStream in,
											int lvl) throws IOException;
} 

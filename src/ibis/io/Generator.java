package ibis.io;

import java.io.IOException;

public abstract class Generator { 
	public abstract Object generated_newInstance(MantaInputStream in) throws ibis.ipl.IbisIOException, ClassNotFoundException;
} 

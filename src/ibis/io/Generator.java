package ibis.io;

import java.io.IOException;

public abstract class Generator { 
	public abstract Object generated_newInstance(IbisSerializationInputStream in) throws IOException;
}

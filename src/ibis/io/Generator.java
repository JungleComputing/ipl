package ibis.io;

public abstract class Generator { 
	public abstract Object generated_newInstance(MantaInputStream in) throws java.io.IOException, ClassNotFoundException;
} 

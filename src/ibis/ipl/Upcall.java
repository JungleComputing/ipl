package ibis.ipl;

import java.io.IOException;

public interface Upcall { 
	public void upcall(ReadMessage m) throws IOException;
} 

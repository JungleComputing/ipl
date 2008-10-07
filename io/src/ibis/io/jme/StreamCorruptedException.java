package ibis.io.jme;

import java.io.IOException;

public class StreamCorruptedException extends IOException {
	private static final long serialVersionUID = 1L;
	public StreamCorruptedException(String msg){
		super(msg);
	}
}

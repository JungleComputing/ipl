package ibis.io.jme;

import java.io.IOException;

public class StreamCorruptedException extends IOException {
	public StreamCorruptedException(String msg){
		super(msg);
	}
}

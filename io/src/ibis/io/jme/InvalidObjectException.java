package ibis.io.jme;

import java.io.IOException;

public class InvalidObjectException extends IOException {
	public InvalidObjectException(String msg){
		super(msg);
	}
}

package ibis.io.jme;

import java.io.IOException;

public class InvalidObjectException extends IOException {
	private static final long serialVersionUID = 1L;
	public InvalidObjectException(String msg){
		super(msg);
	}
}

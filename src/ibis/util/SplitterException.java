package ibis.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class SplitterException extends IOException {

	private ArrayList streams = new ArrayList();
	private ArrayList exceptions = new ArrayList();

	public SplitterException() {
	}

	public void add(OutputStream s, Exception e) {
		streams.add(s);
		exceptions.add(e);
	}

	public int count() {
		return streams.size();
	}

	public OutputStream getStream(int pos) {
		return (OutputStream) streams.get(pos);
	}

	public Exception getException(int pos) {
		return (Exception) exceptions.get(pos);
	}

	public String toString() {
		String res = "got " + streams.size() + " exceptions: ";
		for(int i=0; i<streams.size(); i++) {
			res += "   " + exceptions.get(i) + "\n";
		}

		return res;
	}
}

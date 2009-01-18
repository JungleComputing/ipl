package ibis.ipl.impl.mx.channels;

import java.io.IOException;

import java.util.ArrayList;

public class CollectedWriteException extends IOException {
	// based on splitterException from ibis.io

	/**
	 * 
	 */
	private static final long serialVersionUID = 8852201113944561908L;

	private ArrayList<WriteChannel> channels = new ArrayList<WriteChannel>();

	private ArrayList<Exception> exceptions = new ArrayList<Exception>();

	public CollectedWriteException() {
		// empty constructor
	}

	public void add(WriteChannel c, Exception e) {
		if (channels.contains(c)) {
			System.err.println("AAA, stream was already in splitter exception");
		}

		channels.add(c);
		exceptions.add(e);
	}

	public int count() {
		return channels.size();
	}

	public WriteChannel[] getChannels() {
		return channels.toArray(new WriteChannel[0]);
	}

	public Exception[] getExceptions() {
		return exceptions.toArray(new Exception[0]);
	}

	public WriteChannel getChannel(int pos) {
		return channels.get(pos);
	}

	public Exception getException(int pos) {
		return exceptions.get(pos);
	}

	public String toString() {
		String res = "got " + channels.size() + " exceptions: ";
		for (int i = 0; i < channels.size(); i++) {
			res += "   " + exceptions.get(i) + "\n";
		}

		return res;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Throwable#printStackTrace()
	 */
	public void printStackTrace() {
		for (int i = 0; i < channels.size(); i++) {
			System.err.println("Exception: " + exceptions.get(i));
			((Exception) exceptions.get(i)).printStackTrace();
		}
	}
}

package ibis.util;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

/** Contract: write to multiple outputstreams.
    when an exception occurs, store it and continue.
    when the data is written to all streams, throw one large exception
    that contains all previous exceptions.
    This way, even when one of the streams dies, the rest will receive the data.
**/
public final class OutputStreamSplitter extends OutputStream {
	static final int START_SIZE = 128;
	private OutputStream [] out;
	private int size, index;
	private static final boolean DEBUG = false;
	private boolean removeOnException = false;

	public OutputStreamSplitter() {
		out = new OutputStream[START_SIZE];
		size = START_SIZE;
		index = 0;
	}

	public OutputStreamSplitter(boolean removeOnException) {
		this();
		this.removeOnException = removeOnException;
	}
			
	public void add(OutputStream s) {
		if (DEBUG) {
			System.err.println("SPLIT: ADDING: " + s + ", index = " + index);
		}
		if (index == size) { 
			OutputStream [] temp = new OutputStream[2*size];
			for (int i=0;i<size;i++) { 
				temp[i] = out[i];
			}
			size = 2*size;
			out = temp;
		} 
		
		out[index++] = s;
	}

	private void remove(int pos) {
		for (int i=pos+1;i<index;i++) { 
			out[i-1] = out[i];
		}
		index--;
	}

	public boolean remove(OutputStream s) {
		boolean found = false;

		for (int i=0;i<index;i++) { 

			if (found) { 
				out[i-1] = out[i];
			} else { 
				if (out[i] == s) { 
					found = true;
				} 
			}						
		}

		if (found) {
			index--;
		}

		return found;
	}

	public void write(int b) throws IOException {
		SplitterException e = null;
		if (DEBUG) {
			System.err.println("SPLIT: writing: " + b);
		}

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b);
			} catch (IOException e2) {
				System.err.println("splitter got exception");
				if (e == null) {
					e = new SplitterException();
				}
				e.add(out[i], e2);
				if(removeOnException) {
					remove(i);
					i--;
				}
			}
		}

		if(e != null) {
			System.err.println("splitter throwing exception");
			throw e;
		}
	}

	public void write(byte[] b) throws IOException {
		SplitterException e = null;
		if (DEBUG) {
			System.err.println("SPLIT: writing: " + b + ", b.lenth = " + b.length);
		}

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b);
			} catch (IOException e2) {
				System.err.println("splitter got exception");
				if (e == null) {
					e = new SplitterException();
				}
				e.add(out[i], e2);
				if(removeOnException) {
					remove(i);
					i--;
				}
			}
		}

		if(e != null) {
			System.err.println("splitter throwing exception");
			throw e;
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		SplitterException e = null;
		if (DEBUG) {
			System.err.println("SPLIT: writing: " + b + ", off = " + off + ", len = " + len);
		}

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b, off, len);
			} catch (IOException e2) {
				System.err.println("splitter got exception");
				if (e == null) {
					e = new SplitterException();
				}
				e.add(out[i], e2);
				if(removeOnException) {
					remove(i);
					i--;
				}
			}
		}

		if(e != null) {
			System.err.println("splitter throwing exception");
			throw e;
		}
	}

	public void flush() throws IOException {
		SplitterException e = null;
		if (DEBUG) {
			System.err.println("SPLIT: flush");
		}

		for (int i=0; i<index; i++) {
			try {
				out[i].flush();
			} catch (IOException e2) {
				System.err.println("splitter got exception");
				if (e == null) {
					e = new SplitterException();
				}
				e.add(out[i], e2);
				if(removeOnException) {
					remove(i);
					i--;
				}
			}
		}

		if(e != null) {
			System.err.println("splitter throwing exception");
			throw e;
		}
	}

	public void close() throws IOException {
		SplitterException e = null;
		if (DEBUG) {
			System.err.println("SPLIT: close");
		}

		for (int i=0; i<index; i++) {
			try {
				out[i].close();
			} catch (IOException e2) {
				System.err.println("splitter got exception");
				if (e == null) {
					e = new SplitterException();
				}
				e.add(out[i], e2);
				if(removeOnException) {
					remove(i);
					i--;
				}
			}
		}

		if(e != null) {
			System.err.println("splitter throwing exception");
			throw e;
		}
	}
}

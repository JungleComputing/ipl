import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

final class OutputStreamSplitter extends OutputStream {

	private OutputStream [] out;
	private int size, index;

	OutputStreamSplitter() { 
		out = new OutputStream[16];
		size = 16;
		index = 0;
	}
			
	void add(OutputStream s) {

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

	boolean remove(OutputStream s) {

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
		IOException e = null;

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b);
			} catch (IOException e2) {
				e = e2;
			}
		}

		if(e != null) {
			throw e;
		}
	}

	public void write(byte[] b) throws IOException {
		IOException e = null;

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b);
			} catch (IOException e2) {
				e = e2;
			}
		}

		if(e != null) {
			throw e;
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		IOException e = null;

		for (int i=0; i<index; i++) {
			try {
				out[i].write(b, off, len);
			} catch (IOException e2) {
				e = e2;
			}
		}

		if(e != null) {
			throw e;
		}
	}

	public void flush() throws IOException {
		IOException e = null;

		for (int i=0; i<index; i++) {
			try {
				out[i].flush();
			} catch (IOException e2) {
				e = e2;
			}
		}

		if(e != null) {
			throw e;
		}
	}

	public void close() throws IOException {

		IOException e = null;

		for (int i=0; i<index; i++) {
			try {
				out[i].close();
			} catch (IOException e2) {
				e = e2;
			}
		}

		if(e != null) {
			throw e;
		}
	}
}



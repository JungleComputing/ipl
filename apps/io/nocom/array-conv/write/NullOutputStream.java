import java.io.IOException;
import java.io.OutputStream;

final class NullOutputStream extends OutputStream { 

	public void close() throws IOException { 
	} 

	public void flush() throws IOException {
	} 

	public void write(byte[] b) throws IOException { 
		len += b.length;
	} 

	public void write(byte[] b, int off, int len) throws IOException { 
		this.len += len;
	} 
	
	public void write(int b) throws IOException { 
		len += 1;
	} 

	int len = 0;
	public int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 
} 

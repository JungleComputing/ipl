import java.io.*;

final class NullOutputStream extends OutputStream { 

	public final void close() throws IOException { 
	} 

	public final void flush() throws IOException {
	} 

	public final void write(byte[] b) throws IOException { 
		len += b.length;
	} 

	public final void write(byte[] b, int off, int len) throws IOException { 
		this.len += len;
	} 
	
	public final void write(int b) throws IOException { 
		len += 1;
	} 

	int len = 0;
	public final int getAndReset() { 
		int temp = len;
		len = 0;
		return temp;
	} 
} 

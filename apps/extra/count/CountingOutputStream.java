import java.io.*;

class CountingOutputStream extends OutputStream { 

	private int bytes_written;
	private OutputStream out;
	
	public CountingOutputStream(OutputStream out) { 
		this.out = out;
		bytes_written = 0;
	} 
	
	public final void close() throws IOException { 
		if (out != null) out.close();
	} 

	public final void flush() throws IOException { 
		if (out != null) out.flush();
	} 
	
	public final void write(byte[] b) throws IOException { 
		bytes_written += b.length;
		if (out != null) out.write(b);
	} 
	
	public final void write(byte[] b, int off, int len) throws IOException { 
		bytes_written += len;
		if (out != null) out.write(b, off, len);
	} 

	public final void write(int b) throws IOException { 
		bytes_written++;
		if (out != null) out.write(b);
	} 
	
	public final int bytesWritten() { 
		return bytes_written;
	} 
} 





import java.io.*;

final class NullOutputStream extends OutputStream { 

	public void close() throws IOException { 
	} 

	public void flush() throws IOException {
	} 

	public void write(byte[] b) throws IOException { 
	} 

	public void write(byte[] b, int off, int len) throws IOException { 
	} 
	
	public void write(int b) throws IOException { 
	} 
} 

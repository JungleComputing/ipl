import ibis.io.*;
import java.io.*;

class Main { 

	public static void main(String [] args) { 

		try { 

			CountingOutputStream c = new CountingOutputStream(null);
			BufferedArrayOutputStream b = new BufferedArrayOutputStream(c);
			IbisSerializationOutputStream m = new IbisSerializationOutputStream(b);
			
			Tree t = new Tree(1023);
			
			m.writeObject(t);
			m.reset();
			m.flush();		
			
			int bytes = c.bytesWritten();
			System.out.println("First writeObject -> " + bytes);
			
			m.writeObject(t);
			m.reset();
			m.flush();					

			int bytes2 = c.bytesWritten() - bytes;
			System.out.println("Second writeObject -> " + bytes2);

			c = new CountingOutputStream(null);
			ObjectOutputStream o = new ObjectOutputStream(c);

			o.writeObject(t);			
			o.reset();
			o.flush();		
			
			bytes = c.bytesWritten();
			System.out.println("First writeObject -> " + bytes);
			
			o.writeObject(t);
			o.reset();
			o.flush();					

			bytes2 = c.bytesWritten() - bytes;
			System.out.println("Second writeObject -> " + bytes2);

		} catch (Exception e) { 
			System.out.println("OOPS" + e);
			e.printStackTrace();
		} 
	} 
} 

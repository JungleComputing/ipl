import ibis.io.*;
import java.io.*;

class Main { 

	public static void main(String [] args) { 

		try { 

			FileOutputStream f = new FileOutputStream("aap");
			BufferedArrayOutputStream b = new BufferedArrayOutputStream(f);
			MantaOutputStream m = new MantaOutputStream(b);
			
			Tree t = new Tree(16*1024);
			
			m.writeObject(t);
			m.flush();		

			FileInputStream fi = new FileInputStream("aap");
			BufferedArrayInputStream bi = new BufferedArrayInputStream(fi);
			MantaInputStream mi = new MantaInputStream(bi);
			
			mi.readObject();

		} catch (Exception e) { 
			System.out.println("OOPS" + e);
			e.printStackTrace();
		} 
	} 
} 

import java.io.Serializable;
import java.io.IOException;

import ibis.io.MantaOutputStream;
import ibis.io.MantaInputStream;

public class Data implements Serializable, ibis.io.Serializable { 

	double value;
        Data next;
	double[] a;

	public Data() { }

	public Data(double value, Data next) { 
                this.value = value;
                this.next  = next;
		a = new double[7];
		for (int i = 0; i < 7; i++) {
		    a[i] = 1.0 / i;
		}
        } 

	public void generated_WriteObject(MantaOutputStream out)
		throws IOException {
System.err.println("Write Data.value " + value);
	    out.writeDouble(value);
System.err.println("Write Data.a " + a);
	    out.writeObject(a);
System.err.println("Write Data.next " + next);
	    out.writeObject(next);
	}

	public void generated_ReadObject(MantaInputStream in)
		throws IOException, ClassNotFoundException {
	    value = in.readDouble();
System.err.println("Read " + this + " Data.value " + value);
	    a = (double[])in.readObject();
System.err.println("Read " + this + " Data.a " + a);
	    next = (Data)in.readObject();
System.err.println("Read " + this + " Data.next " + next);
	}
} 

import ibis.gmi.*;

public class TPCombiner extends FlatInvocationCombiner { 

    byte [] buffer;
	boolean nocopy;
	

    TPCombiner(int size, boolean nocopy) { 
	buffer = new byte[size];
	this.nocopy = nocopy;
    } 

    public void combine(ParameterVector [] in, ParameterVector out) {
	int len = in.length;
	int offset = 0;
	byte [] temp;

	if (nocopy) { 
		for (int i=0;i<len;i++) { 
			temp = (byte []) in[i].readObject(0);
		} 
	} else { 
		for (int i=0;i<len;i++) { 
			temp = (byte []) in[i].readObject(0);
			System.arraycopy(temp, 0, buffer, offset, temp.length);
			offset += temp.length;
		} 
	}

	out.write(0, buffer);
    } 
} 

import ibis.gmi.*;

public class TPCombiner extends FlatInvocationCombiner { 

	byte [] buffer;
	int size;	
	
    TPCombiner(int size) {
	buffer = new byte[size];
	this.size = size;
    }   

    public void combine(ParameterVector [] in, ParameterVector out) {
	int len = in.length;
	int frag = size/ len;
	int offset = 0;
	byte [] temp;

	for (int i=0;i<len;i++) { 
		temp = (byte []) in[i].readObject(0);
		System.arraycopy(temp, 0, buffer, offset, frag);
		offset += frag;
	} 

	out.write(0, buffer);
    } 
} 

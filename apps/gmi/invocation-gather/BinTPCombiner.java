import ibis.gmi.*;

public class BinTPCombiner extends BinomialInvocationCombiner { 

    boolean copy;

    BinTPCombiner(boolean copy) {
	this.copy = copy;
    }   

    public void combine(ParameterVector in1, ParameterVector in2, ParameterVector out) {
	byte [] temp1;
	byte [] temp2;

	temp1 = (byte []) in1.readObject(0);
	temp2 = (byte []) in2.readObject(0);

	byte [] buffer = new byte[temp1.length + temp2.length];

	if (copy) {
	    System.arraycopy(temp1, 0, buffer, 0, temp1.length);
	    System.arraycopy(temp2, 0, buffer, temp1.length, temp2.length);
	}

	out.write(0, buffer);
    } 
} 

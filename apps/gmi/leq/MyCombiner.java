import ibis.gmi.*;

public class MyCombiner extends FlatInvocationCombiner { 

	double [] result;
	double residue;

	MyCombiner(int size) { 
		result = new double[size];
	} 

	public void combine(ParameterVector [] in, ParameterVector out) { 
		// each machine submits a double [] and a residue value....
		int start = 0;
		residue = 0.0;
		
		for (int i=0;i<in.length;i++) { 
			double [] temp = (double [])in[i].readObject(0);
			residue += in[i].readDouble(1);
			System.arraycopy(temp, 0, result, start, temp.length);
			start += temp.length;
		}

		out.write(0, result);
		out.write(1, residue);
	}
} 

import ibis.gmi.*;

public class MyCombiner extends FlatInvocationCombiner { 

	MyCombiner() { 
	} 

	public void combine(ParameterVector [] in, ParameterVector out) { 
		// each machine submits a PivotElt....
		PivotElt solution = (PivotElt) in[0].readObject(0);
		
		for (int i=1;i<in.length;i++) { 
			PivotElt elt = (PivotElt)in[i].readObject(0);

			if (elt.cols < elt.max_cols) {
				if (solution.cols < solution.max_cols) {
					if (elt.norm > solution.norm) {
						solution.max_over_max_cols = elt.norm;
						solution.index = elt.index;
						solution.cols  = elt.cols;
					}
				} else {
					solution.max_over_max_cols = elt.norm;
					solution.index = elt.index;
					solution.cols  = elt.cols;
				}
			} else if (solution.cols >= solution.max_cols) {
				solution.max_over_max_cols = 0.0;
				if (elt.norm > solution.norm) {
					solution.index = elt.index;
				}
			}
			
			if (elt.norm > solution.norm) {
				solution.norm = elt.norm;
			}
		}
		out.write(0, solution);
	}
} 

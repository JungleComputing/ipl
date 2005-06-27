/*
 * Created on 13.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the operation: minimum local (MPJ.MINLOC).
 * Only available for these datatypes: MPJ.SHORT2, MPJ.INT2, MPJ.LONG2, MPJ.FLOAT2, MPJ.DOUBLE2.
 */
public class OpMinLoc extends Op {
	OpMinLoc(boolean commute) throws MPJException {
		super(commute);
	}

	public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException {
		if(datatype == MPJ.SHORT2) {
			if (((short[])invec).length != ((short[])inoutvec).length) {
				return;
			}
			
			int dispIndexIn  = inoffset;
			int dispIndexOut = outoffset;
		      
			for (int i = 0; i < count; i++) {
				short o1 = ((short[])invec)[dispIndexIn];
		      	short o2 = ((short[])inoutvec)[dispIndexOut];

		        if(o1 < o2) {
		          ((short[])inoutvec)[dispIndexOut] = o1;
		          ((short[])inoutvec)[dispIndexOut+1] = ((short[])invec)[dispIndexIn+1] ;
		        }
		        else if(o1 == o2) {
		        	if(((short[])invec)[dispIndexIn+1] < ((short[])inoutvec)[dispIndexOut+1]) {
		        		((short[])inoutvec)[dispIndexOut+1] = ((short[])invec)[dispIndexIn+1];
		        	}
		        }		        
		        dispIndexIn += 2;
		        dispIndexOut += 2;
			}
			return;
		}
		else if(datatype == MPJ.INT2) {
			if (((int[])invec).length != ((int[])inoutvec).length) {
				return;
			}
			
			int dispIndexIn  = inoffset;
			int dispIndexOut = outoffset;
		      
			for (int i = 0; i < count; i++) {
				int o1 = ((int[])invec)[dispIndexIn];
		      	int o2 = ((int[])inoutvec)[dispIndexOut];

		        if(o1 < o2) {
		          ((int[])inoutvec)[dispIndexOut] = o1;
		          ((int[])inoutvec)[dispIndexOut+1] = ((int[])invec)[dispIndexIn+1] ;
		        }
		        else if(o1 == o2) {
		        	if(((int[])invec)[dispIndexIn+1] < ((int[])inoutvec)[dispIndexOut+1]) {
		        		((int[])inoutvec)[dispIndexOut+1] = ((int[])invec)[dispIndexIn+1];
		        	}
		        }
		        dispIndexIn += 2;
		        dispIndexOut += 2;
			}
			return;
		}
		else if(datatype == MPJ.LONG2) {
			if (((long[])invec).length != ((long[])inoutvec).length) {
				return;
			}
			
			int dispIndexIn  = inoffset;
			int dispIndexOut = outoffset;
		      
			for (int i = 0; i < count; i++) {
				long o1 = ((long[])invec)[dispIndexIn];
		      	long o2 = ((long[])inoutvec)[dispIndexOut];

		        if(o1 < o2) {
		          ((long[])inoutvec)[dispIndexOut] = o1;
		          ((long[])inoutvec)[dispIndexOut+1] = ((long[])invec)[dispIndexIn+1] ;
		        }
		        else if(o1 == o2) {
		        	if(((long[])invec)[dispIndexIn+1] < ((long[])inoutvec)[dispIndexOut+1]) {
		        		((long[])inoutvec)[dispIndexOut+1] = ((long[])invec)[dispIndexIn+1];
		        	}
		        }
		        dispIndexIn += 2;
		        dispIndexOut += 2;
			}
			return;
		}
		else if(datatype == MPJ.FLOAT2) {
			if (((float[])invec).length != ((float[])inoutvec).length) {
				return;
			}
			
			int dispIndexIn  = inoffset;
			int dispIndexOut = outoffset;
		      
			for (int i = 0; i < count; i++) {
				float o1 = ((float[])invec)[dispIndexIn];
		      	float o2 = ((float[])inoutvec)[dispIndexOut];

		        if(o1 < o2) {
		          ((float[])inoutvec)[dispIndexOut] = o1;
		          ((float[])inoutvec)[dispIndexOut+1] = ((float[])invec)[dispIndexIn+1] ;
		        }
		        else if(o1 == o2) {
		        	if(((float[])invec)[dispIndexIn+1] < ((float[])inoutvec)[dispIndexOut+1]) {
		        		((float[])inoutvec)[dispIndexOut+1] = ((float[])invec)[dispIndexIn+1];
		        	}
		        }
		        dispIndexIn += 2;
		        dispIndexOut += 2;
			}
			return;
		}
		else if(datatype == MPJ.DOUBLE2) {
			if (((double[])invec).length != ((double[])inoutvec).length) {
				return;
			}
			
			int dispIndexIn  = inoffset;
			int dispIndexOut = outoffset;
		      
			for (int i = 0; i < count; i++) {
				double o1 = ((double[])invec)[dispIndexIn];
		      	double o2 = ((double[])inoutvec)[dispIndexOut];

		        if(o1 < o2) {
		          ((double[])inoutvec)[dispIndexOut] = o1;
		          ((double[])inoutvec)[dispIndexOut+1] = ((double[])invec)[dispIndexIn+1] ;
		        }
		        else if(o1 == o2) {
		        	if(((double[])invec)[dispIndexIn+1] < ((double[])inoutvec)[dispIndexOut+1]) {
		        		((double[])inoutvec)[dispIndexOut+1] = ((double[])invec)[dispIndexIn+1];
		        	}
		        }
		        dispIndexIn += 2;
		        dispIndexOut += 2;
			}
			return;
		}
	
		throw new MPJException("Operation does not support this Datatype");
	}
}

// File: $Id$

// Representation of a single solution of the SAT problem.
class SATSolution implements java.io.Serializable {
    int pos[];
    int neg[];

    // Given an assignment vector, construct a solution instance.
    // Because Java does not allow arrays to be grown, we first
    // count the number of elements in the pos and neg arrays,
    // and then create and fill them.
    public SATSolution( int assignments[] ){
	int pos[];
	int neg[];
	int posix = 0;
	int negix = 0;

	// First count them.
        for( int ix=0; ix<assignments.length; ix++ ){
	    int a = assignments[ix];

	    if( a == 0 ){
	        negix++;
	    }
	    else if( a == 1 ){
	        posix++;
	    }
	}

	// Create the arrays.
	pos = new int[posix];
	neg = new int[negix];

	// Fill the arrays.
	posix = 0;
	negix = 0;
        for( int ix=0; ix<assignments.length; ix++ ){
	    int a = assignments[ix];

	    if( a == 0 ){
	        neg[negix++] = ix;
	    }
	    else if( a == 1 ){
	        pos[posix++] = ix;
	    }
	}
	this.pos = pos;
	this.neg = neg;
    }

    public String toString()
    {
        String res = "";
	boolean first = true;

	for( int ix=0; ix<pos.length; ix++ ){
	    if( !first ){
	        res += " ";
	    }
	    else {
	        first = false;
	    }
	    res += pos[ix];
	}
	for( int ix=0; ix<neg.length; ix++ ){
	    if( !first ){
	        res += " ";
	    }
	    else {
	        first = false;
	    }
	    res += "!" + neg[ix];
	}
	return res;
    }
}

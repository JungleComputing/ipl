// $Id$
//
// The genes of a problem.
// This is just a bunch of arrays of sensible gene types, and some helper
// functions on genes.


class Genes {
    public float floats[];
    public int ints[];
    public boolean bools[];

    public Genes( float f[], int i[], boolean b[] ){
	floats = f;
	ints = i;
	bools = b;
    }

    /**
     * Returns a clone of this vector. The copy will contain a reference to a
     * clone of the internal data array, not a reference to the internal data
     * array of this IntVector object.
     */
    public Object clone() {
	float f[] = floats;
	int i[] = ints;
	boolean b[] = bools;

	if( f != null ){
	    f = (float[]) f.clone();
	}
	if( i != null ){
	    i = (int[]) i.clone();
	}
	if( b != null ){
	    b = (boolean[]) b.clone();
	}
	return new Genes( f, i, b );
    }

    public String toString()
    {
	String res = "";

	if( floats != null && floats.length != 0 ){
	    boolean first = true;
	    res += "[";
	    for( int i=0; i<floats.length; i++ ){
		if( first ){
		    first = false;
		}
		else {
		    res += ' ';
		}
		res += floats[i];
	    }
	    res += "]";
	}
	if( ints != null && ints.length != 0 ){
	    boolean first = true;
	    res += "[";
	    for( int i=0; i<ints.length; i++ ){
		if( first ){
		    first = false;
		}
		else {
		    res += ' ';
		}
		res += ints[i];
	    }
	    res += "]";
	}
	if( bools != null && bools.length != 0 ){
	    boolean first = true;
	    res += "[";
	    for( int i=0; i<bools.length; i++ ){
		if( first ){
		    first = false;
		}
		else {
		    res += ' ';
		}
		res += bools[i]?'1':'0';
	    }
	    res += "]";
	}
	return res;
    }
}

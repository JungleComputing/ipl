// File: $Id$

// A replacement step, represented as a list of text positions and a length.
class Step {
    int occurences[];
    int len;

    public Step( int occ[], int n, int l )
    {
        occurences = new int[n];

        System.arraycopy( occ, 0, occurences, 0, n );
        len = l;
    }

    public String toString()
    {
        String res = "([";

        for( int i=0; i<occurences.length; i++ ){
            if( i != 0 ){
                res += ",";
            }
            res += occurences[i];
        }
        return res + "],len=" + len + ")";
    }

    public int getGain()
    {
	int gain = occurences.length*(len-1);
	int loss = len+1;
	System.out.println( "gain=" + gain + ", loss=" + loss );
	return gain-loss;
    }
}

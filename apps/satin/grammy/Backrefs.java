// File: $Id$


class Backrefs {
    static final int ALPHABETSIZE = 256;

    // Given a byte array, build an array of backreferences. That is,
    // construct an array that at each text position gives the index
    // of the previous occurence of that hash code, or -1 if there is none.
    private static int[] buildBackrefs( byte text[] )
    {
        int heads[] = new int[ALPHABETSIZE];
        int backrefs[] = new int[text.length];
        int trails[] = new int[text.length];

        // Lengths are initialized on zero.
        for( int i=0; i<ALPHABETSIZE; i++ ){
            heads[i] = -1;
        }
        for( int i=0; i<text.length; i++ ){
            int hashcode = (int) text[i];
            backrefs[i] = heads[hashcode];
            heads[hashcode] = i;
        }
        int slot = 0;
        for( int i=0; i<heads.length; i++ ){
            if( heads[i] != -1 ){
                trails[slot++] = heads[i];
            }
        }
        while( slot<trails.length ){
            trails[slot++] = -1;
        }
        return trails;
    }

    public static void main( String args[] )
    {
        String sample = "a long long time ago in a galaxy far away";

        int trails[] = buildBackrefs( sample.getBytes() );
    }
}

// File: $Id$
//
// The text index.
//
// It contains two parts: a list of filenames, and a HashMap on words,
// with for each word a list of indices in the list of filenames.

import java.util.TreeMap;

public class Index implements java.io.Serializable {
    protected String files[];
    protected final TreeMap wordOccurences = new TreeMap();
    private static final boolean compactSpans = true;
    private static final boolean showSpanCompaction = false;

    public Index()
    {
        files = new String[0];
    }

    /**
     * Constructs a new Index with only the given file in it, and containing
     * only word occurcences to the given set of words.
     * @param fnm The file that was indexed.
     * @param s The set of words in that file.
     */
    public Index( String fnm, java.util.SortedSet s )
    {
        files = new String[] { fnm };

        java.util.Iterator it = s.iterator();

        while( it.hasNext() ){
            String w = (String) it.next();
            wordOccurences.put( w, new int[] { 0 } );
        }
    }

    private static void dumpArray( int l[] )
    {
        System.err.print( '[' );
        for( int i=0; i<l.length; i++ ){
            if( i>0 ){
                System.err.print( ' ' );
            }
            System.err.print( l[i] );
        }
        System.err.print( ']' );
    }

    /**
     * Given two occurence lists, returns a merged occurence list.
     */
    private static int []mergeOccurenceLists( int l1[], int offset, int l2[] )
    {
        if( l2 == null || l2.length == 0 ){
            return l1;
        }
        for( int i=0; i<l2.length; i++ ){
            if( l2[i]<0 ){
                l2[i] -= offset;
            }
            else {
                l2[i] += offset;
            }
        }
        if( l1 == null || l1.length == 0 ){
            return l2;
        }
        if( showSpanCompaction ){
            dumpArray( l1 );
            dumpArray( l2 );
        }
        // First see if we can do something clever with spans of
        // occurences.
        int skip1 = 0;
        int skip2 = 0;
        int lastix1 = l1.length-1;
        int last1 = l1[lastix1];
        int first2 = l2[0];
        if( compactSpans ){
            if( (last1>=0) && last1+1 == first2 ){
                // Turn this in a sort span.
                l2[0] = -l2[0];
                if( l2.length>1 && l2[1]<0 ){
                   // We already had a span, merge our stuff in by deleting
                   // entry l2[0].
                   skip2 = 1;
                }
            }
            else if( (last1<0) && (-last1)+1 == first2 ){
                // Turn this in a sort span.
                l2[0] = -l2[0];
                skip1 = 1;  // We're not interested in the end of the span.
                if( l2.length>1 && l2[1]<0 ){
                   // We already had a span, merge our stuff in by deleting
                   // entry l2[0].
                   skip2 = 1;
                }
            }
        }
        int nwl[] = new int[l1.length+l2.length-(skip1+skip2)];
        System.arraycopy( l1, 0, nwl, 0, l1.length-skip1 );
        System.arraycopy( l2, skip2, nwl, l1.length-skip1, l2.length-skip2 );
        if( showSpanCompaction ){
            System.err.print( "->" );
            dumpArray( nwl );
            System.err.println();
        }
        return nwl;
    }

    /**
     * Adds the index entries in the given Index to our own.
     * This is done by concatenating the lists of files, and 
     * merging the occurence lists of the two entries.
     * @param ix The index to merge with.
     */
    public void merge( Index ix )
    {
        if( ix == null ){
            return;
        }
        // The offset of the newly added entries in our index.
        int offset = files.length;

        // First, enlarge the list of files.
        String nwfiles[] = new String[offset+ix.files.length];
        System.arraycopy( files, 0, nwfiles, 0, files.length );
        System.arraycopy( ix.files, 0, nwfiles, offset, ix.files.length );
        files = nwfiles;

        // Now add all word occurrences of `ix' to our own set.
        // Renumber all merged in occurrences to use the index that
        // we use.
        java.util.Iterator it = ix.wordOccurences.keySet().iterator();
        while( it.hasNext() ){
            String key = (String) it.next();
            int l1[] = (int[]) wordOccurences.get( key );
            int l2[] = (int[]) ix.wordOccurences.get( key );
            int l[] = mergeOccurenceLists( l1, offset, l2 );
            if( l != null && l != l1 ){
                wordOccurences.put( key, l );
            }
        }
    }

    // Write the index to the given Writer.
    public void write( java.io.Writer w ) throws java.io.IOException
    {
        for( int i=0; i<files.length; i++ ){
            w.write( files[i] );
            w.write( '\n' );
        }

        java.util.Iterator it = wordOccurences.keySet().iterator();
        while( it.hasNext() ){
            String key = (String) it.next();
            w.write( key );
            w.write( ':' );
            int prev = -1;
            int l[] = (int[]) wordOccurences.get( key );
            for( int i=0; i<l.length; i++ ){
                int v = l[i];

                if( v<0 && prev+1 == -v ){
                    // A span of two elements is booooring
                    v = -v;
                }
                w.write( " " + v );
                prev = v;
            }
            w.write( '\n' );
        }
	w.close();
    }
}

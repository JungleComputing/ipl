// File: $Id$
//
// The text index.
//
// It contains two parts: a list of filenames, and a HashMap on words,
// with for each word a list of indices in the list of filenames.

import java.util.HashMap;

public class Index implements java.io.Serializable {
    protected String files[];
    protected final HashMap wordOccurences = new HashMap();

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

    /**
     * Merges the given Index with our own.
     * @param ix The index to merge with.
     */
    public void add( Index ix )
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
            int l1[] = (int[]) ix.wordOccurences.get( key );
            for( int i=0; i<l1.length; i++ ){
                l1[i] += offset;
            }
            Object e = wordOccurences.get( key );
            if( e == null ){
                // The word only occurs in the merged in entry.
                // Create a new entry.
                wordOccurences.put( key, l1 );
            }
            else {
                // We also have entries. Concatenate them.
                int l[] = (int []) e;

                int nwl[] = new int[l.length+l1.length];
                System.arraycopy( l, 0, nwl, 0, l.length );
                System.arraycopy( l1, 0, nwl, l.length, l1.length );
                wordOccurences.put( key, nwl );
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
            int l[] = (int[]) wordOccurences.get( key );
            for( int i=0; i<l.length; i++ ){
                w.write( " " + l[i] );
            }
            w.write( '\n' );
        }
    }
}

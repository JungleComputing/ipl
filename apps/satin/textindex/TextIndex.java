// File: $Id$

/**
 * A parallel text indexer. Given two directories, traverse the first
 * directory, and construct files in the second directory with just the
 * words that occur in the files of the first directory.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.TreeSet;

public final class TextIndex extends ibis.satin.SatinObject implements IndexerInterface, java.io.Serializable {
    private static final boolean traceTreeWalk = false;
    private static final Pattern nonWord = Pattern.compile( "\\W+" );

    /**
     * Given the name of a file to index, returns an Index entry for just
     * that file.
     * @param f The file to index.
     */
    public Index indexFile( String f ) throws IOException
    {
        BufferedReader r = new BufferedReader( new FileReader( f ) );
        TreeSet set = new TreeSet();

        while( true ){
            String s = r.readLine();
            if( s == null ){
                break;
            }
            String words[] = nonWord.split( s );
            for( int i=0; i<words.length; i++ ){
                String w = words[i];

                if( w.length() != 0 ){
                    set.add( w.toLowerCase() );
                }
            }
        }
        r.close();
        return new Index( f, set );
    }

    public Index indexDirectory( String dirnm ) throws IOException
    {
        File dir = new File( dirnm );

        String files[] = dir.list();
        Index ix[] = new Index[files.length];

        for( int i=0; i<files.length; i++ ){
            String fnm = files[i];

            if( fnm.charAt( 0 ) != '.' ){
                // Skip '.', '..', and all hidden files per Unix convention.
                File f = new File( dir, fnm );

                if( f.isDirectory() ){
                    if( traceTreeWalk ){
                        System.err.println( "Visiting directory " + f );
                    }
                    ix[i] = indexDirectory( f.toString() );
                }
                else if( f.isFile() ){
                    if( traceTreeWalk ){
                        System.err.println( "Indexing plain file " + f );
                    }
                    ix[i] = indexFile( f.toString() );
                }
                else {
                    System.err.println( "Skipping weird file " + f );
                }
            }
        }
        sync();
        Index res = new Index();
        for( int i=0; i<ix.length; i++ ){
            res.add( ix[i] );
        }
        return res;
    }

    /**
     * Deletes the specified file or directory, and all files
     * under it.
     * @param f The file or directory to delete.
     */
    private static boolean delTree( File f )
    {
        boolean success = true;
        if( !f.exists() ){
            return true;
        }
        if( f.isFile() ){
            success &= f.delete();
        }
        else if( f.isDirectory() ){
            // First delete any files in the directory.
            String files[] = f.list();

            for( int ix=0; ix<files.length; ix++ ){
                String fnm = files[ix];

                if( fnm.charAt( 0 ) != '.' ){
                    // Skip '.', '..', and all hidden files per Unix convention.
                    File f1 = new File( f, fnm );

                    success &= delTree( f1 );
                }
            }
            success &= f.delete();
        }
        else {
            System.err.println( "Cannot delete weird file " + f );
            success = false;
        }
        return success;
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
	if( args.length != 2 ){
	    System.err.println( "An input and an output directory are required, but I have " + args.length + ":" );
            for( int i=0; i<args.length; i++ ){
                System.err.println( " [" + i + "] "  + args[i] );
            }
	    System.exit( 1 );
	}
	File dir = new File( args[0] );
	File ixfile = new File( args[1] );
	if( !dir.exists() ){
	    System.err.println( "The directory to index does not exist: " + dir );
	    System.exit( 1 );
	}
        if( !dir.isDirectory() ){
	    System.err.println( "The directory to index is not a directory: " + dir );
	    System.exit( 1 );
        }

        System.out.println( "Indexing " + dir + " to " + ixfile );

	long startTime = System.currentTimeMillis();
        TextIndex ix = new TextIndex();
        Index res = ix.indexDirectory( dir.toString() );
        ix.sync();
        res.write( new FileWriter( ixfile ) );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
    }
}

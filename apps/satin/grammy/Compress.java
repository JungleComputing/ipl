// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject
{
    static boolean doVerification = false;

    public ByteBuffer compress( byte text[] )
    {
	SuffixArray a = new SuffixArray( text );
	ByteBuffer out = a.compress();
        a.printGrammar();
        return out;
    }

    static void usage()
    {
        System.err.println( "Usage: [-verify] [-depth <n>] <text> <compressedtext>" );
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
	File infile = null;
	File outfile = null;

        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-verify" ) ){
                doVerification = true;
            }
            else if( args[i].equals( "-depth" ) ){
                i++;
                //lookahead_depth = Integer.parseInt( args[i] );
            }
            else if( infile == null ){
                infile = new File( args[i] );
            }
            else if( outfile == null ){
                outfile = new File( args[i] );
            }
            else {
                usage();
                System.exit( 1 );
            }
        }
        if( infile == null || outfile == null ){
            usage();
            System.exit( 1 );
        }
        byte text[] = Helpers.readFile( infile );
	long startTime = System.currentTimeMillis();

        Compress c = new Compress();

        ByteBuffer buf = c.compress( text );

        Helpers.writeFile( outfile, buf );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.getLength() + " bytes." );
        if( doVerification ){
            ByteBuffer debuf = Decompress.decompress( buf );
            byte nt[] = debuf.getText();

            if( nt.length != text.length ){
                System.out.println( "Error: decompressed text has different length from original. Original is " + text.length + " bytes, decompression is " + nt.length + " bytes" );
                System.exit( 1 );
            }
            for( int i=0; i<nt.length; i++ ){
                if( nt[i] != text[i] ){
                    System.out.println( "Error: decompressed text differs from original at position " + i );
                    System.exit( 1 );
                }
            }
        }
    }
}

// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject
{
    static final boolean traceMatches = false;
    static final boolean traceLookahead = false;
    static final boolean traceSiteCount = false;
    static final boolean parallelizeShallowEvaluation = false;
    static int lookahead_depth = Configuration.LOOKAHEAD_DEPTH;
    static boolean doVerification = false;

    public Rule compressRange( byte text[], int from, int to )
    {
        if( from == to ){
            return null;
        }
        if( from+1 == to ){
            return new Rule( text[from] );
        }
        int mid = (from+to)/2;
        return new Rule(
            compressRange( text, from, mid ),
            compressRange( text, mid, to )
        );
    }

    public ByteBuffer compress( byte text[] )
    {
        Rule n = compressRange( text, 0, text.length );

        ByteBuffer out = new ByteBuffer();
        return out;
    }

    static void usage()
    {
        System.err.println( "Usage: [-verify] [-short <n>] [-depth <n>] <text> <compressedtext>" );
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
            else if( args[i].equals( "-short" ) ){
                i++;
            }
            else if( args[i].equals( "-depth" ) ){
                i++;
                lookahead_depth = Integer.parseInt( args[i] );
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

        //System.out.println( "Recursion depth: " + lookahead_depth + ", max. shortening: " + max_shortening  );

        Compress c = new Compress();

        ByteBuffer buf = c.compress( text );

        Helpers.writeFile( outfile, buf );

	long endTime = System.currentTimeMillis();
	double time = ((double) (endTime - startTime))/1000.0;

	System.out.println( "ExecutionTime: " + time );
        System.out.println( "In: " + text.length + " bytes, out: " + buf.sz + " bytes." );
        if( doVerification ){
            // ByteBuffer debuf = Decompress.decompress( buf );
            ByteBuffer debuf = null;
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

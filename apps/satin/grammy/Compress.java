// File: $Id$

import java.io.File;

class Compress extends ibis.satin.SatinObject implements Configuration, CompressorInterface
{
    static boolean doVerification = false;
    private int top;
    private int lookahead;

    public Compress( int top, int lookahead )
    {
        this.top = top;
	this.lookahead = lookahead;
    }

    /**
     * Applies the given step in the compression process, and any
     * subsequent steps that are also helpful.
     */
    public SuffixArray applyFoldingStep( SuffixArray a, Step s, int levels )
        throws VerificationException
    {
        a.applyCompression( s );
	if( levels>0 ){
	    a = applyFolding( a, levels );
	}
        return a;
    }

    /**
     * Given a (possibly compressed) text with surrounding information,
     * try to find the optimal compression.
     * @return The compressed text.
     */
    private SuffixArray applyFolding( SuffixArray a, int levels ) throws VerificationException
    {
        SuffixArray res;

        if( a.getLength() == 0 ){
            return a;
        }
        StepList steps = a.selectBestSteps( top );

	if( steps.getLength() == 0 ){
	    return a;
	}

        // TODO: even if top is larger than 1, there may be only one
        // possibility.
	if( levels<2 || top == 1 ){
	    Step s = steps.getBestStep();
	    res = (SuffixArray) a.clone();

	    if( s != null ){
		res.applyCompression( s );
	    }
	}
	else {
	    Step mv[] = steps.toArray();
	    SuffixArray l[] = new SuffixArray[mv.length];

	    for( int i=0; i<mv.length; i++ ){
		l[i] = applyFoldingStep( (SuffixArray) a.clone(), mv[i], levels-1 );
	    }
	    sync();
	    res = l[0];
            int len = res.getLength();
	    for( int i=1; i<l.length; i++ ){
                int leni = l[i].getLength();

		if( leni<len ){
		    res = l[i];
                    len = leni;
		}
	    }
	}
        return res;
    }

    /** Returns a compressed version of the string represented by
     * this suffix array.
     */
    public ByteBuffer compress( SuffixArray a ) throws VerificationException
    {
	int startLength;
	do {
	    // Keep trying to do `lookahead' steps of compression
	    // until there no longer is progress.
	    startLength = a.getLength();
	    a = applyFolding( a, lookahead );
	    if( traceIntermediateGrammars ){
		a.printGrammar();
	    }
	} while( a.getLength()<startLength );
        return a.getByteBuffer();
    }

    public ByteBuffer compress( byte text[] ) throws VerificationException
    {
	SuffixArray a = new SuffixArray( text );
	return compress( a );
    }

    static void usage()
    {
        System.err.println( "Usage: [-quiet] [-verify] [-lookahead <n>] [-top <n>] <text-file> <compressed-file>" );
	System.err.println( "   or: [-quiet] [-verify] [-lookahead <n>] [-top <n>] -string <text-string> <compressed-file>" );
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
	File infile = null;
	File outfile = null;
        int top = DEFAULT_TOP;
        int lookahead = DEFAULT_LOOKAHEAD;
        String intext = null;
        boolean quiet = false;
        boolean compression = true;

        for( int i=0; i<args.length; i++ ){
            if( args[i].equals( "-verify" ) ){
                doVerification = true;
            }
            else if( args[i].equals( "-nocompress" ) ){
                compression = false;
            }
            else if( args[i].equals( "-quiet" ) ){
                quiet = true;
            }
            else if( args[i].equals( "-lookahead" ) ){
                i++;
                lookahead = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( "-top" ) ){
                i++;
                top = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( "-string" ) ){
                i++;
                if( intext != null || infile != null ){
                    System.err.println( "More than one text to compress given" );
                    usage();
                    System.exit( 1 );
                }
                intext = args[i];
            }
            else if( infile == null ){
                infile = new File( args[i] );
            }
            else if( outfile == null ){
                outfile = new File( args[i] );
            }
            else {
                System.err.println( "Superfluous parameter `" + args[i] + "'" );
                usage();
                System.exit( 1 );
            }
        }
        if( intext == null && (infile == null || outfile == null) ){
            usage();
            System.exit( 1 );
        }

        try {
            byte text[];
	    if( infile != null ){
		text = Helpers.readFile( infile );
	    }
	    else if( intext != null ){
		text = intext.getBytes();
	    }
	    else {
		System.err.println( "No text to compress" );
		usage();
		System.exit( 1 );
		text = null;
	    }
            long startTime = System.currentTimeMillis();
            ByteBuffer buf;

            if( compression ){
                Compress c = new Compress( top, lookahead );
                buf = c.compress( text );
            }
            else {
                // Only test the byte convesion.
                SuffixArray a = new SuffixArray( text );
                buf = a.getByteBuffer();
            }
            if( outfile != null ){
                Helpers.writeFile( outfile, buf );
            }

            long endTime = System.currentTimeMillis();
            double time = ((double) (endTime - startTime))/1000.0;

            if( !quiet ){
                System.out.println( "ExecutionTime: " + time );
                System.out.println( "In: " + text.length + " bytes, out: " + buf.getLength() + " bytes." );
            }
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
        catch( Exception x ){
            x.printStackTrace();
        }
    }
}

// File: $Id$

/**
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;
import java.util.Random;

public final class Breeder {
    static final int MAXGEN = 400;

    // Given genes, and old, slightly worse genes, an extrapolated clone.
    private static Genes extrapolateGenes( float scale, Genes g, Genes oldG, Genes max, Genes min )
    {
	Genes res = (Genes) g.clone();

	// We blindly assume oldG has same existence and length als g arrays.
	if( g.floats != null ){
	    float f[] = g.floats;
	    float fo[] = oldG.floats;

	    for( int ix=0; ix<f.length; ix++ ){
		float delta = f[ix]-fo[ix];

		res.floats[ix] += scale*delta;
		if( res.floats[ix]<min.floats[ix] ){
		    res.floats[ix] = min.floats[ix];
		}
		if( res.floats[ix]>max.floats[ix] ){
		    res.floats[ix] = max.floats[ix];
		}
	    }
	}
	// For the boolean array nothing useful can be done.

	// TODO: also do something for the ints.
	return res;
    }

    // Given genes, return a mutated clone (hur, hur).
    private static Genes mutateGenes( Random rng, Genes g, float step, Genes max, Genes min )
    {
	Genes res = (Genes) g.clone();
	int ix = rng.nextInt( res.floats.length );
	if( ix<g.floats.length ){
	    res.floats[ix] += ((step/2.0f) - (step*rng.nextFloat()));
	    if( res.floats[ix]<min.floats[ix] ){
		res.floats[ix] = min.floats[ix];
	    }
	    if( res.floats[ix]>max.floats[ix] ){
		res.floats[ix] = max.floats[ix];
	    }
	}
	return res;
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {

	if( args.length == 0 ){
	    System.err.println( "A list of filename arguments required." );
	    System.exit( 1 );
	}

	Random rng = new Random( 2 );
	SATProblem pl[] = new SATProblem[args.length];

	for( int i=0; i<args.length; i++ ){
	    File f = new File( args[i] );
	    if( !f.exists() ){
		System.err.println( "File does not exist: " + f );
		System.exit( 1 );
	    }
	    SATProblem p = SATProblem.parseDIMACSStream( f );
	    System.err.println( "Problem file: " + args[i] );
	    p.report( System.out );
	    p.optimize();
	    p.report( System.out );
	    pl[i] = p;
	}

	Genes maxGenes = BreederSolver.getMaxGenes();
	Genes minGenes = BreederSolver.getMinGenes();

	Genes genes = BreederSolver.getInitialGenes();

	Genes bestGenes = genes;
	Genes prevBestGenes = null;
	int bestD = BreederSolver.run( pl, genes, Integer.MAX_VALUE );
	if( bestD == -1 ){
	    System.err.println( "Initial run is hopeless???" );
	    System.exit( 1 );
	}
	System.err.println( "Decisions: " + bestD );
	System.err.println( "Initial (" + bestD + ") " + bestGenes );
	float step = 0.2f;

	for( int gen = 0; gen<MAXGEN; gen++ ){
	    Genes nextGenes;
	    boolean extrapolating = false;

	    if( prevBestGenes != null ){
		nextGenes = extrapolateGenes( 0.3f, bestGenes, prevBestGenes, maxGenes, minGenes );
		System.err.println( "Extrapolating ..." );
		extrapolating = true;
	    }
	    else {
		nextGenes = mutateGenes( rng, bestGenes, step, maxGenes, minGenes );
	    }
	    prevBestGenes = null;
	    int nextD = BreederSolver.run( pl, nextGenes, (3*bestD)/2 );
	    if( nextD<0 ){
		// Way out of line. Perhaps a smaller step leads to something
		// useful.
		if( extrapolating ){
		    System.err.println( "Hopeless" );
		}
		else {
		    step *= 0.98;
		    System.err.println( "Hopeless; step=" + step );
		}
	    }
	    else {
		System.err.print( "Decisions: " + nextD );
	    
		if( nextD == bestD ){
		    // Nothing interesting happens
		    if( !extrapolating ){
			step *= 1.02;
			System.err.print( " step=" + step );
		    }
		}
		System.err.println();
		if( nextD<bestD ){
		    bestD = nextD;
		    prevBestGenes = bestGenes;
		    bestGenes = nextGenes;
		    System.err.println( "New optimum (" + bestD + ") " + bestGenes );
		}
	    }
	}
	System.out.println( "Best result (" + bestD + ") " + bestGenes );
    }
}

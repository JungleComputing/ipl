// File: $Id$

/**
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;
import java.util.Random;


public final class Breeder extends ibis.satin.SatinObject implements BreederInterface {
    static final int GENERATIONS = 20;
    static final int GENERATION_SIZE = 20;
    static final boolean globalsInTuple = true;
    static final boolean printOptimizerStats = true;
    static final float mutationStep = 0.15f;

    private final Random rng = new Random( 2 );
    Genes maxGenes = BreederSolver.getMaxGenes();
    Genes minGenes = BreederSolver.getMinGenes();

/*
    static {
        System.setProperty( "satin.tuplespace.multicast", "true" );
    }
*/
    
/* @@@ We might be able to use an active object here
    static final class CutoffUpdater implements ibis.satin.ActiveTuple {
        int limit;

        CutoffUpdater( int v ){
            limit = v;
        }

        public void handleTuple( String key ){
            if( limit<Breeder.cutoff ){
                Breeder.cutoff = limit;
            }
        }
    }
*/
    Breeder() { }

    /** Maximal number decisions allowed before we give up. Can
     * be updated by the CutoffUpdater class above.
     */
    static int cutoff = Integer.MAX_VALUE;

    static class Result {
        Genes genes;    // The genes used.
        int decisions;  // The number of decisions needed.

        public Result( Genes g, int d )
        {
            genes = g;
            decisions = d;
        }

        public String toString()
        {
            return "(" + decisions + ") " + genes;
        }
    };


    /**
     * Given a list of SAT problems and a set of genes, returns the
     * number of decisions needed to solve these problems. The
     * static variable <code>cutoff</code> is honoured: if more than
     * that number of decisions are needed to solve the problem, give
     * give up.
     * @param pl The list of problems to use.
     * @param genes The configuration to use.
     * @return The number decisions needed, or -1 if that count is
     * larger than <code>cutoff</code>.
     */
    public int solve( SATProblem pl[], Genes genes )
    {
        int total = 0;
        if( cutoff == 0 ){
            cutoff = Integer.MAX_VALUE;
        }
/*             // @@@ we might get it from a shared object instead
        if( pl == null ){
            pl = (SATProblem []) ibis.satin.SatinTupleSpace.get( "problem" );
        }
        */
        try {
            for( int i=0; i<pl.length; i++ ){
                int d = BreederSolver.run( pl[i], genes, cutoff-total );
                total += d;
            }
        }
        catch( SATCutoffException x ){
             return -1;
        }
        int newCutoff = 3*total/2;
        if( cutoff>newCutoff ){
            // We may have a new cutoff value, broadcast it.
//            SatinTupleSpace.add( "cutoff", new CutoffUpdater( newCutoff ) );
        }
        return total;
    }
 
    // Given genes, and old, slightly worse genes, generate an
    // extrapolated clone.
    private static Genes extrapolateGenes( float scale, Genes g, Genes oldG, Genes max, Genes min )
    {
        Genes res = (Genes) g.clone();

        // We blindly assume oldG has same null-s and array lengths as g.
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

    private Genes prevBestGenes = null;

    /**
     * Breed the next generation.
     */
    private Result breedNextGeneration( Random rng, SATProblem pl[], Genes genes, int bestD )
    {
        float step = 0.15f;
        int res[] = new int[GENERATION_SIZE];
        Genes g[] = new Genes[GENERATION_SIZE];
        int slot = 0;

        cutoff = (3*bestD)/2;
        if( prevBestGenes != null ){
            // If we have changed best genes, fill one trial slot
            // with an extrapolation of the change in genes.
            g[slot] = extrapolateGenes( 0.3f, genes, prevBestGenes, maxGenes, minGenes );
            res[slot] = solve( pl, g[slot] );
            slot++;
        }
        prevBestGenes = null;
        for( int i=slot; i<GENERATION_SIZE; i++ ){
            // Fill all remaining slots with mutations
            float s1 = (i<GENERATION_SIZE/2)?step:2*step;
            g[i] = mutateGenes( rng, genes, s1, maxGenes, minGenes );
            //System.err.println( "Genes = " + g[i] );
            res[i] = solve( pl, g[i] );
        }
        sync();

        // Now evaluate the results.
        for( int i=0; i<GENERATION_SIZE; i++ ){
            int nextD = res[i];

            if( nextD>=0 ){
                System.err.print( nextD + " " );
                if( nextD<bestD ){
                    bestD = nextD;
                    prevBestGenes = genes;
                    genes = g[i];
                }
            }
            else {
                System.err.print( "** " );
            }
        }
        System.err.println();
        return new Result( genes, bestD );
    }

    public void run( SATProblem pl[] ){

        Genes bestGenes = BreederSolver.getInitialGenes();
        int bestD = Integer.MAX_VALUE;

        for( int gen = 0; gen<GENERATIONS; gen++ ){
            Result res = breedNextGeneration( rng, pl, bestGenes, bestD );
            bestGenes = res.genes;
            bestD = res.decisions;
            System.err.println( "g" + gen + "->" + res );
        }
        System.out.println( "Best result (" + bestD + ") " + bestGenes );
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

        SATProblem pl[] = new SATProblem[args.length];

        System.err.println( Helpers.getPlatformVersion() );
        System.err.println( "Generations=" + GENERATIONS );
        System.err.println( "Generation size=" + GENERATION_SIZE );
        System.err.println( "Globals in tuple=" + globalsInTuple );
        for( int i=0; i<args.length; i++ ){
            File f = new File( args[i] );
            if( !f.exists() ){
                System.err.println( "File does not exist: " + f );
                System.exit( 1 );
            }
            SATProblem p = SATProblem.parseDIMACSStream( f );
            System.err.println( "Problem file: " + args[i] );
            p.report( System.out );
            p.optimize( printOptimizerStats );
            p.report( System.out );
            pl[i] = p;
        }
/*             // @@@ we might get it from a shared object instead

        if( globalsInTuple ){
            ibis.satin.SatinTupleSpace.add( "problem", pl );
            pl = null;
        }
*/
        Breeder b = new Breeder();

        long startTime = System.currentTimeMillis();

        b.run( pl );

        long endTime = System.currentTimeMillis();
        double time = ((double) (endTime - startTime))/1000.0;

        System.out.println( "ExecutionTime: " + time );

        System.out.print( "application time " + "Breeder (");
        for (int i = 0; i < args.length; i++) {
            System.out.print(args[i] + " ");
        }
        System.out.println(") took " + time + " seconds");
    }
}

// File: $Id$

/**
 * A sequential SAT solver specifically used in evolution. Given a symbolic
 * boolean equation in CNF, find a set of assignments that make this
 * equation true.
 * 
 * This implementation tries to do all the things a professional SAT
 * solver would do, although we are limited by implementation time and
 * the fact that we need to parallelize the stuff.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;

public final class BreederSolver {
    private static final boolean traceSolver = false;
    private static final boolean printSatSolutions = false;
    private static final boolean traceNewCode = true;
    private static final boolean traceLearning = false;
    private static final boolean traceRestarts = false;
    private static int label = 0;

    /** Total number of decisions in all solves. */
    private int decisions = 0;

    private int cutoff = 0;

    private static final int GENECOUNT = 8;

    /**
     * Solve the leaf part of a SAT problem.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level The branching level.
     * @param p The SAT problem to solve.
     * @param ctx The changable context of the solver.
     * @param var The next variable to assign.
     * @param val The value to assign.
     */
    public void leafSolve(
	int level,
	SATProblem p,
	SATContext ctx,
	int var,
	boolean val
    ) throws SATResultException, SATRestartException, SATCutoffException
    {
	int res = ctx.update( p, level, false );
	if( res == SATProblem.CONFLICTING ){
	    if( traceSolver ){
		System.err.println( "ls" + level + ": update found a conflict" );
	    }
	    return;
	}
	if( res == SATProblem.SATISFIED ){
	    // Propagation reveals problem is satisfied.
	    SATSolution s = new SATSolution( ctx.assignment );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "ls" + level + ": update found a solution: " + s );
	    }
	    if( !p.isSatisfied( ctx.assignment ) ){
		System.err.println( "Error: " + level + ": solution does not satisfy problem." );
	    }
	    throw new SATResultException( s );
	}
	ctx.assignment[var] = val?(byte) 1:(byte) 0;
	if( traceSolver ){
	    System.err.println( "ls" + level + ": trying assignment var[" + var + "]=" + ctx.assignment[var] );
	}
	if( val ){
	    res = ctx.propagatePosAssignment( p, var, level, false );
	}
	else {
	    res = ctx.propagateNegAssignment( p, var, level, false );
	}
	if( res == SATProblem.CONFLICTING ){
	    if( traceSolver ){
		System.err.println( "ls" + level + ": propagation found a conflict" );
	    }
	    return;
	}
	if( res == SATProblem.SATISFIED ){
	    // Propagation reveals problem is satisfied.
	    SATSolution s = new SATSolution( ctx.assignment );

	    if( traceSolver | printSatSolutions ){
		System.err.println( "ls" + level + ": propagation found a solution: " + s );
	    }
	    if( !p.isSatisfied( ctx.assignment ) ){
		System.err.println( "Error: " + level + ": solution does not satisfy problem." );
	    }
	    throw new SATResultException( s );
	}
	int nextvar = ctx.getDecisionVariable();
	if( nextvar<0 ){
	    // There are no variables left to assign, clearly there
	    // is no solution.
	    if( traceSolver ){
		System.err.println( "ls" + level + ": nothing to branch on" );
	    }
	    return;
	}
        decisions++;
	if( decisions>cutoff ){
	    throw new SATCutoffException();
	}

	boolean firstvar = ctx.posDominant( nextvar );
	SATContext subctx = (SATContext) ctx.clone();
        try {
            leafSolve( level+1, p, subctx, nextvar, firstvar );
        }
        catch( SATRestartException x ){
	    if( x.level<level ){
                if( traceRestarts ){
                    System.err.println( "RestartException passes level " + level + " heading for level " + x.level );
                }
		throw x;
	    }
        }
	// Since we won't be using our context again, we may as well
	// give it to the recursion.
        // However, we must update the administration with any
        // new clauses that we've learned recently.
	leafSolve( level+1, p, ctx, nextvar, !firstvar );
    }

    /**
     * Given a SAT problem, returns a solution, or <code>null</code> if
     * there is no solution.
     * @param p The problem to solve.
     * @param cutoff The maximum number of decisions to try.
     * @return A solution of the problem, or <code>null</code> if there is no solution.
     */
    protected SATSolution solveSystem( final SATProblem p )
	throws SATCutoffException
    {
	SATSolution res = null;

	if( p.isConflicting() ){
	    return null;
	}
	if( p.isSatisfied() ){
	    return new SATSolution( p.buildInitialAssignments() );
	}
	int oldClauseCount = p.getClauseCount();

        // Now recursively try to find a solution.
	try {
	    SATContext ctx = SATContext.buildSATContext( p );

	    ctx.assignment = p.buildInitialAssignments();

	    int r = ctx.optimize( p );
	    if( r == SATProblem.SATISFIED ){
		if( !p.isSatisfied( ctx.assignment ) ){
		    System.err.println( "Error: solution does not satisfy problem." );
		}
		return new SATSolution( ctx.assignment );
	    }
	    if( r == SATProblem.CONFLICTING ){
		return null;
	    }

	    int nextvar = ctx.getDecisionVariable();
	    if( nextvar<0 ){
		// There are no variables left to assign, clearly there
		// is no solution.
		if( traceSolver | traceNewCode ){
		    System.err.println( "top: nothing to branch on" );
		}
		return null;
	    }
	    if( traceSolver ){
		System.err.println( "Top level: branching on variable " + nextvar );
	    }
            decisions++;

	    SATContext negctx = (SATContext) ctx.clone();
	    boolean firstvar = ctx.posDominant( nextvar );
            try {
                leafSolve( 0, p, negctx, nextvar, firstvar );
            }
            catch( SATRestartException x ){
                if( x.level<0 ){
                    if( traceRestarts ){
                        System.err.println( "RestartException reaches top level, no solutions" );
                    }
                    return null;
                }
            }
            leafSolve( 0, p, ctx, nextvar, !firstvar );
	}
	catch( SATResultException r ){
	    res = r.s;
	    if( res == null ){
		System.err.println( "A null result thrown???" );
	    }
            return res;
	}
        catch( SATRestartException x ){
	    if( traceRestarts ){
		System.err.println( "RestartException reaches top level: no solutions" );
	    }
	    // No solution found.
            res = null;
        }

	int newClauseCount = p.getClauseCount();
	return res;
    }

    static Genes getInitialGenes()
    {
	float g[] = new float[GENECOUNT];

	for( int i=0; i<g.length; i++ ){
	    g[i] = 1.0f;
	}
	return new Genes( g, null, null );
    }

    static Genes getMaxGenes()
    {
	float g[] = new float[GENECOUNT];

	for( int i=0; i<g.length; i++ ){
	    g[i] = 100.0f;
	}
	return new Genes( g, null, null );
    }

    static Genes getMinGenes()
    {
	float g[] = new float[GENECOUNT];

	for( int i=0; i<g.length; i++ ){
	    g[i] = 1e-6f;
	}
	return new Genes( g, null, null );
    }

    /**
     * Given a SAT problem, a set of genes, and an upper limit
     * for the number of decisions to try, solve the given problem,
     * and count the number of decisions that were required for it.
     * @param p_in The SAT problem to solve.
     * @param genes The configuration values to use.
     * @param cutoff The maximum number of decisions to try before giving up.
     * @return The number of decisions needed, or -1 if we're over
     * the cutoff limit.
     */
    static int run( final SATProblem p_in, Genes genes, int cutoff )
       throws SATCutoffException
    {
        BreederSolver s = new BreederSolver();
        s.cutoff = cutoff;

	SATProblem p = (SATProblem) p_in.clone();
	p.reviewer = new GeneticClauseReviewer( genes.floats );
        s.solveSystem( p );
	return s.decisions;
    }

}

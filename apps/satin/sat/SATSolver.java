// File: $Id$

/**
 * A parallel SAT solver. Given a symbolic boolean equation in CNF, find a set
 * of assignments that make this equation true.
 * 
 * This implementation tries to do all the things a professional SAT
 * solver would do, although we are limited by implementation time and
 * the fact that we need to parallelize the stuff.
 * 
 * @author Kees van Reeuwijk
 * @version $Revision$
 */

import java.io.File;

public final class SATSolver extends ibis.satin.SatinObject implements SATInterface, java.io.Serializable {
    private static final boolean traceSolver = false;
    private static final boolean printSatSolutions = true;
    private static final boolean printOptimizerStats = true;
    private static final boolean traceNewCode = true;
    private static final boolean traceLearning = false;
    private static final boolean traceJumps = false;
    static SATProblem p = null;

    SATSolver( SATProblem p ){
    }

    /**
     * Solve the leaf part of a SAT problem.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level The branching level.
     * @param ctx The changable context of the solver.
     * @param var The next variable to assign.
     * @param val The value to assign.
     * @param learnTuple Propagate any learned clauses as active tuple?
     */
    public void leafSolve(
        int level,
        SATContext ctx,
        int var,
        boolean val,
        boolean learnTuple
    ) throws SATResultException, SATJumpException
    {
        int res = ctx.update( p, level );
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
        if( traceSolver ){
            System.err.println( "ls" + level + ": trying assignment var[" + var + "]=" + val );
        }
        if( ctx.assignment[var] != -1 ){
            int a = val?1:0;

            if( ctx.assignment[var] != a ){
                // Learned clauses imply an assignment that conflicts
                // with our assignment. Give up.
                return;
            }
        }
        else {
            if( val ){
                res = ctx.propagatePosAssignment( p, var, level, learnTuple, true );
            }
            else {
                res = ctx.propagateNegAssignment( p, var, level, learnTuple, true );
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

        boolean firstvar = ctx.posDominant( nextvar );
        SATContext subctx = (SATContext) ctx.clone();
        try {
            leafSolve( level+1, subctx, nextvar, firstvar, learnTuple );
        }
        catch( SATJumpException x ){
            if( x.level<level ){
                if( traceJumps ){
                    System.err.println( "JumpException passes level " + level + " heading for level " + x.level );
                }
                throw x;
            }
        }
        // Since we won't be using our context again, we may as well
        // give it to the recursion.
        leafSolve( level+1, ctx, nextvar, !firstvar, learnTuple );
    }

    /**
     * The method that implements a Satin task.
     * The method throws a SATResultException if it finds a solution,
     * or terminates normally if it cannot find a solution.
     * @param level The branching level.
     * @param localLevel The number of generations on this processor.
     * @param ctx The changable context of the solver.
     * @param var The next variable to assign.
     * @param val The value to assign.
     * @param learnTuple Propagate any learned clauses as active tuple?
     */
    public void solve(
        int level,
        int localLevel,
        SATContext ctx,
        int var,
        boolean val,
        boolean learnTuple
    ) throws SATException
    {
        if( !localJob() ){
            // This job was stolen, reset the local generations counter.
            localLevel = 0;
        }
        int res = ctx.update( p, level );
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
        if( traceSolver ){
            System.err.println( "s" + level + ": trying assignment var[" + var + "]=" + val );
        }
        if( ctx.assignment[var] != -1 ){
            // There already is an assignment.
            int a = val?1:0;
            if( ctx.assignment[var] != a ){
                // The assignment implied by the learned clauses conflicts
                // with what we have. We may as well stop.
                return;
            }
        }
        else {
            if( val ){
                res = ctx.propagatePosAssignment( p, var, level, learnTuple, true );
            }
            else {
                res = ctx.propagateNegAssignment( p, var, level, learnTuple, true );
            }
            if( res == SATProblem.CONFLICTING ){
                // Propagation reveals a conflict.
                if( traceSolver ){
                    System.err.println( "s" + level + ": propagation found a conflict" );
                }
                return;
            }
            if( res == SATProblem.SATISFIED ){
                // Propagation reveals problem is satisfied.
                SATSolution s = new SATSolution( ctx.assignment );

                if( traceSolver | printSatSolutions ){
                    System.err.println( "s" + level + ": propagation found a solution: " + s );
                }
                if( !p.isSatisfied( ctx.assignment ) ){
                    System.err.println( "Error: " + level + ": solution does not satisfy problem." );
                }
                throw new SATResultException( s );
            }
        }
        int nextvar = ctx.getDecisionVariable();
        if( nextvar<0 ){
            // There are no variables left to assign, clearly there
            // is no solution.
            if( traceSolver ){
                System.err.println( "s" + level + ": nothing to branch on" );
            }
            return;
        }

        boolean firstvar = ctx.posDominant( nextvar );

        if( localLevel<10 ){
            try {
                // We have variable 'nextvar' to branch on.
                SATContext firstctx = (SATContext) ctx.clone();
                solve( level+1, localLevel+1, firstctx, nextvar, firstvar, learnTuple );
                SATContext secondctx = (SATContext) ctx.clone();
                solve( level+1, localLevel+1, secondctx, nextvar, !firstvar, learnTuple );
                sync();
            }
            catch( SATJumpException x ){
                if( x.level<level ){
                    if( traceJumps ){
                        System.err.println( "JumpException passes level " + level + " heading for level " + x.level );
                    }
                    throw x;
                }
            }
        }
        else {
            // We're nearly there, use the leaf solver.
            // We have variable 'nextvar' to branch on.
            SATContext subctx = (SATContext) ctx.clone();
            try {
                leafSolve( level+1, subctx, nextvar, firstvar, learnTuple );
            }
            catch( SATJumpException x ){
                if( x.level<level ){
                    if( traceJumps ){
                        System.err.println( "JumpException passes level " + level + " heading for level " + x.level );
                    }
                    throw x;
                }
                // We have an untried value, continue with that.
            }
            subctx = (SATContext) ctx.clone();
            leafSolve( level+1, subctx, nextvar, !firstvar, learnTuple );
        }
    }

    /**
     * Given a SAT problem, returns a solution, or <code>null</code> if
     * there is no solution.
     * @param p The problem to solve.
     * @return A solution of the problem, or <code>null</code> if there is no solution.
     */
    static SATSolution solveSystem( final SATProblem p, boolean learnTuple )
    {
        SATSolution res = null;

        if( p.isConflicting() ){
            return null;
        }
        if( p.isSatisfied() ){
            return new SATSolution( p.buildInitialAssignments() );
        }
        int oldClauseCount = p.getClauseCount();
        SATSolver s = new SATSolver( p );

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

            // Put the problem in the Satin tuple space.
//            ProblemAssigner a = new ProblemAssigner(p);
//            ibis.satin.SatinTupleSpace.add( "problem",  a );

            SATContext negctx = (SATContext) ctx.clone();
            boolean firstvar = ctx.posDominant( nextvar );

            s.solve( 0, 0, negctx, nextvar, firstvar, learnTuple );
            s.solve( 0, 0, ctx, nextvar, !firstvar, learnTuple );
            s.sync();
        }
        catch( SATResultException r ){
            res = r.s;
            s.abort();
            if( res == null ){
                System.err.println( "A null result thrown???" );
            }
            return res;
        }
        catch( SATJumpException x ){
            if( traceJumps ){
                System.err.println( "JumpException reaches top level, no solutions" );
            }
            s.abort();
            return null;
        }
        catch( SATException x ){
            System.err.println( "Uncaught " + x + "???" );
        }

        int newClauseCount = SATSolver.p.getClauseCount();
        System.err.println( "Learned " + (newClauseCount-oldClauseCount) + " clauses." );
        return res;
    }

    /**
     * Allows execution of the class.
     * @param args The command-line arguments.
     */
    public static void main( String args[] ) throws java.io.IOException
    {
        if( args.length != 1 ){
            String msg = "Exactly one filename argument required, but I have " + args.length + ":";
            for( int i=0; i<args.length; i++ ){
                msg += " [" + i + "] "  + args[i];
            }
            throw new IllegalArgumentError( msg );
        }
        File f = new File( args[0] );
        if( !f.exists() ){
            throw new IllegalArgumentError( "File does not exist: " + f );
        }

        // Turn Satin temporarily off to prevent slowdowns of
        // sequential code.
        ibis.satin.SatinObject.pause(); 

        System.err.println( Helpers.getPlatformVersion() );
        SATProblem p = SATProblem.parseDIMACSStream( f );
        p.setReviewer( new CubeClauseReviewer() );
        p.report( System.out );
        p.optimize( printOptimizerStats );
        p.report( System.out );

        // Turn Satin on again
        ibis.satin.SatinObject.resume();

        long startTime = System.currentTimeMillis();
        SATSolution res = solveSystem( p, true );

        long endTime = System.currentTimeMillis();
        double time = ((double) (endTime - startTime))/1000.0;

        p.report( System.out );
        System.out.println( "ExecutionTime: " + time );

        System.out.println( "application time " + "SATSolver (" + args[0] + ") took " + time + " seconds");
        if( res == null ){
            System.out.println( "There are no solutions" );
        }
        else {
            System.out.println( "There is a solution: " + res );
        }
    }
}

// File: $Id$

/**
 * The exception that is thrown when the SAT solver wants to do a restart.
 * The specified level is the one where the search should continue.
 */
class SATRestartException extends SATException {
    int level;

    SATRestartException( int l ){ level = l; }
}

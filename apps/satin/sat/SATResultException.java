// File: $Id$

/** The exception that is thrown when the SAT solver finds a solution.
 * The exception mechanism is used in Satin to abort a divide-and-conquer
 * problem prematurely. The exception carries the result.
 */
class SATResultException extends Exception {
    SATSolution s;

    /** Construct an exception with the given solution.
     * @param s The solution to carry in this exception.
     */
    SATResultException( SATSolution s ){ this.s = s; }
}

// File: $Id$

/**
 * The exception that is thrown when the SAT solver wants to do a restart.
 */
class SATJumpException extends SATException {
    int level;

    SATJumpException(int level) {
        this.level = level;
    }
}
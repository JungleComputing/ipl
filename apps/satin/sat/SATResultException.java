// File: $Id$

class SATResultException extends Exception {
    SATSolution s;

    SATResultException( SATSolution s ){ this.s = s; }
}

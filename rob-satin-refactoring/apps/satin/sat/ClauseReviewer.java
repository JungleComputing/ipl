// $Id$
//
// Abstract base class for clause weighting methods.

abstract class ClauseReviewer implements java.io.Serializable {
    abstract float info( int n );

    float info( Clause c ) { return info( c.pos.length+c.neg.length ); }

}

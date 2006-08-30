/* $Id$ */

import ibis.satin.WriteMethodsInterface;

public interface SATProblemInterface extends WriteMethodsInterface {
    public int addClause( int pos[], int neg[] );
    public void addConflictClause( Clause cl );
}

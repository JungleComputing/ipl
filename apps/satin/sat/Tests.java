// File: $Id$

import junit.framework.TestCase;

public class Tests extends TestCase {
    final static int empty[] = new int[0];
    final static int a[] = { 1, 2 };
    final static int b[] = { 1, 2 };
    final static int c[] = { 3 };

    public void testClauseClone()
    {
        Clause cl = new Clause( a, b, 5 );
        Clause cl1 = (Clause) cl.clone();
        assertEquals( cl, cl1 );
    }

    public void testClauseResolution()
    {
	Clause c1 = new Clause( new int[] { 0, 1 }, new int[] { 2, 3 }, 0 );
	Clause c2 = new Clause( new int[] { 0, 2 }, new int[] { 3 }, 1 );
	Clause res = new Clause( new int[] { 0, 1 }, new int[] { 3 }, -1 );

	assertEquals( res, Clause.resolve( c1, c2, 2 ) );
    }

    public void testEqualArrays()
    {
	assertTrue( Helpers.areEqualArrays( a, a ) );
	assertTrue( Helpers.areEqualArrays( a, b ) );
	assertFalse( Helpers.areEqualArrays( a, c ) );
	assertFalse( Helpers.areEqualArrays( b, c ) );
	assertFalse( Helpers.areEqualArrays( empty, c ) );
    }

    public void testHelperContains()
    {
	assertTrue( Helpers.contains( a, 1 ) );
	assertFalse( Helpers.contains( a, 3 ) );
	assertFalse( Helpers.contains( empty, 1 ) );
    }

    public void testHelperAppend()
    {
	int x[] = new int[] { 1 };

	assertFalse( Helpers.areEqualArrays( x, a ) );
	x = Helpers.append( x, 2 );
	assertTrue( Helpers.areEqualArrays( x, a ) );
	x = Helpers.append( x, 2 );
	assertFalse( Helpers.areEqualArrays( x, a ) );
    }

    private void runASort( int l[] )
    {
	Helpers.sortIntArray( l );
	assertTrue( Helpers.isIncreasing( l ) );
    }

    public void testSort()
    {
	assertTrue( Helpers.isIncreasing( a ) );
	runASort( new int[0] );
	runASort( new int[1] );
	runASort( new int[] { 1, 2, 0, -1, 5 } );
	runASort( new int[] { 1, 2, 3 } );
    }

    public void testSATVarClone()
    {
        SATVar v = new SATVar( 1 );
        SATVar v1 = (SATVar) v.clone();
        assertEquals( v, v1 );
    }

}

// $Id$

// Provide a resizable array of ints.
// Note that we can't extend/implement the abstract container classes, since
// they assume Object elements, while the whole point of this class is
// to provide precise typing and an efficient array representation.
// We try to adhere to the naming scheme of these classes, though.
class IntVector {
    protected int capacityIncrement;
    protected int elementCount;
    protected int elementData[];

    IntVector( int initialCapacity, int capacityIncrement  ){
        this.capacityIncrement = capacityIncrement;
	elementData = new int[initialCapacity];
	elementCount = 0;
    }
    IntVector( int initialCapacity ){ this( initialCapacity, 0 ); }
    IntVector(){ this( 10, 0 ); }

    int capacity() { return elementData.length; }
    
    void clear() { elementCount = 0; }

    int elementAt( int ix ) { return elementData[ix]; }

    void ensureCapacity( int minCap )
    {
        if( elementData.length<minCap ){
	    int newCapacity = elementData.length+capacityIncrement;
	    if( capacityIncrement<=0 ){
	        newCapacity += elementData.length;
	    }
	    if( newCapacity<minCap ){
	        newCapacity = minCap;
	    }
	    int arr[] = new int[newCapacity];

	    System.arraycopy( elementData, 0, arr, 0, elementData.length );
	    elementData = arr;
	}
    }

    int get( int ix ) { return elementData[ix]; }

    int set( int ix, int val )
    {
        int oldval = elementData[ix];

	elementData[ix] = val;
	return oldval;
    }

    int size() { return elementData.length; }
    
    boolean add( int val )
    {
	ensureCapacity( elementCount+1 );
	elementData[elementCount++] = val;
        return true;
    }
}

// $Id$

/**
 * A resizable array of ints. Note that we can't extend/implement the
 * abstract container classes, since they assume Object elements, while the
 * whole point of this class is to provide precise typing and an efficient
 * array representation. We try to adhere to the naming scheme of these
 * classes, though.
 */
public final class IntVector implements java.io.Serializable, Cloneable {
    /** The array containing the elements. */
    protected int elementData[];

    /**
     * Constructs an empty vector.
     */
    public IntVector() {
	elementData = new int[0];
    }

    /**
     * Constructs a vector containing the elements of the specified vector.
     * 
     * @param v the vector
     */
    public IntVector( IntVector v ) {
        elementData = new int[v.size()];
	System.arraycopy( v.elementData, 0, elementData, 0, v.size() );
    }

    /**
     * Constructs a vector containing the specified range of elements of the
     * specified vector.
     * 
     * @param v the vector
     * @param from the first element to copy
     * @param to the first element not to copy
     */
    public IntVector( IntVector v, int from, int to ) {
	int length = (to < from) ? 0 : to - from;
        elementData = new int[length];
	System.arraycopy( v, from, elementData, 0, length );
    }

    /**
     * Returns a clone of this vector. The copy will contain a reference to a
     * clone of the internal data array, not a reference to the internal data
     * array of this IntVector object.
     */
    public Object clone() {
	return new IntVector( this );
    }

    /** Returns the current capacity of this vector. */
    public int capacity() {
	return elementData.length;
    }

    /** Removes all elements from this vector. */
    void clear() {
	elementData = new int[0];
    }

    /**
     * Returns the component at the specified index.
     * 
     * @param index an index into this vector
     */
    public int elementAt( int index ) {
	return elementData[index];
    }

    /**
     * Returns the component at the specified index.
     * 
     * @param index an index into this vector
     */
    public int get( int index ) {
	return elementData[index];
    }

    public int set( int ix, int val ) {
	int oldval = elementData[ix];

	elementData[ix] = val;
	return oldval;
    }

    /**
     * @return The number of elements of this vector. 
     */
    public int size() {
	return elementData.length;
    }

    /**
     * Tests if this vector has no components.
     * 
     * @return true if and only if this vector has no elements; false
     *         otherwise.
     */
    public boolean isEmpty() {
	return elementData.length == 0;
    }

    /**
     * Appends the specified element ot the end of this vector.
     * 
     * @param val the element to be appended to this vector
     * @return true (in analogy to the method in java.util.Vector)
     */
    public boolean add( int val ) {
        int nw[] = new int[elementData.length+1];
        int sz = elementData.length;

	System.arraycopy( elementData, 0, nw, 0, sz );
        nw[sz] = val;
        elementData = nw;
	return true;
    }

    /**
     * Tests wether a specified element occurs in this vector.
     * @param v the element to search for
     * @return true iff the specified element occurs in this vector.
     */
    public boolean contains( int v ) {
	for( int i = 0; i < elementData.length; i++ ){
	    if( elementData[i] == v ){
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns an array containing the elements of this vector.
     * @return An array with the elements of this vector.
     */
    public int[] toArray() {
	return Helpers.cloneIntArray( elementData, elementData.length );
    }

    /**
     * Returns a string representation of this vector.
     * @return The string.
     */
    public String toString() {
	String res = "[";
	boolean first = true;

	for( int i = 0; i < elementData.length; i++ ){
	    if( first ){
		first = false;
	    } else{
		res += ",";
	    }
	    res += elementData[i];
	}

	return res + "]";
    }
}

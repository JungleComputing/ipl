// $Id$

/** A resizable array of ints.
 * Note that we can't extend/implement the abstract container classes, since
 * they assume Object elements, while the whole point of this class is
 * to provide precise typing and an efficient array representation.
 * We try to adhere to the naming scheme of these classes, though.
 */
public final class IntVector implements java.io.Serializable, Cloneable {
    /**
     * The amount by which the capacity of this vector is automatically
     * incremented when its size becomes grater than its capacity.
     */
    protected int capacityIncrement;

    /** The number of valid elements in this vector. */
    protected int elementCount;

    /** The array containing the elements. */
    protected int elementData[];

    /**
     * Constructs an empty vector with the specified initial capacity
     * and capacity increment.
     * @param initialCapacity the initial capacity of this vector
     * @param capacityIncrement the amount by which the capacity is increased when this vector overflows
     */
    public IntVector( int initialCapacity, int capacityIncrement  ){
        this.capacityIncrement = capacityIncrement;
	elementData = new int[initialCapacity];
	elementCount = 0;
    }

    /**
     * Constructs and empty vector with the specified initial capacity
     * and with its capacity increment equal to zero.
     * @param initialCapacity the initial capacity of this vector
     */
    public IntVector( int initialCapacity ){ this( initialCapacity, 0 ); }

    /** Constructs an empty vector so that is internal data array has
     * size 10 and its standard capacity increment is zero.
     */
    public IntVector(){ this( 10, 0 ); }

    /**
     * Constructs a vector containing the elements of the specified
     * vector.
     * @param v the vector 
     */
    public IntVector( IntVector v ){
	this( v.size(), 0 );
	System.arraycopy( v, 0, elementData, 0, v.size() );
    }

    /**
     * Constructs a vector containing the specified range of elements of the
     * specified vector.
     * @param v the vector 
     * @param from the first element to copy
     * @param to the first element not to copy
     */
    public IntVector( IntVector v, int from, int to ){
	this( (to<from)?0:to-from, 0 );
	int length = (to<from)?0:to-from;
	System.arraycopy( v, from, elementData, 0, length );
	elementCount = length;
    }

    /**
     * Returns a clone of this vector. The copy will contain a reference
     * to a clone of the internal data array, not a reference to the
     * internal data array of this IntVector object.
     */
    public Object clone()
    {
	return new IntVector( this );
    }

    /** Returns the current capacity of this vector. */
    public int capacity() { return elementData.length; }
    
    /** Removes all elements from this vector. */
    void clear() { elementCount = 0; }

    /**
     * Increases the capacity of this vector, if necessary, to ensure that
     * it can hold at least the number of ints specified by the minimum
     * capacity parameter.
     * <p>
     * If the current capacity of this vector is less than minCapacity, then
     * its capacity is increased by replacing its internal data array,
     * kept in the field elementData, with a laarger one. This size of the
     * new data array will be the old size plus the capacityIncrement, unless
     * the value of capacityIncrement is less than or equal to zero, in
     * which case the new capacity will be minCapacity.
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity( int minCapacity )
    {
        if( elementData.length<minCapacity ){
	    int newCapacity = elementData.length+capacityIncrement;
	    if( capacityIncrement<=0 ){
	        newCapacity += elementData.length;
	    }
	    if( newCapacity<minCapacity ){
	        newCapacity = minCapacity;
	    }
	    int arr[] = new int[newCapacity];

	    System.arraycopy( elementData, 0, arr, 0, elementData.length );
	    elementData = arr;
	}
    }

    /**
     * Returns the component at the specified index.
     * @param index an index into this vector
     */
    public int elementAt( int index ) { return elementData[index]; }

    /**
     * Returns the component at the specified index.
     * @param index an index into this vector
     */
    public int get( int index ) { return elementData[index]; }

    public int set( int ix, int val )
    {
        int oldval = elementData[ix];

	elementData[ix] = val;
	return oldval;
    }

    /** Returns the number of elements of this vector. */
    public int size() { return elementCount; }

    /**
     * Tests if this vector has no components.
     * @return true if and only if this vector has no elements; false otherwise.
     */
    public boolean isEmpty() { return elementCount == 0; }
    
    /**
     * Appends the specified element ot the end of this vector.
     * @param val the element to be appended to this vector
     * @return true (in analogy to the method in java.util.Vector)
     */
    public boolean add( int val )
    {
	ensureCapacity( elementCount+1 );
	elementData[elementCount++] = val;
        return true;
    }

    /**
     * Returns the internal array containing the elements of this vector.
     * Note that the length of the array may be more than the value returned
     * by size().
     */
    public int[] toArray() { return elementData; }

    /**
     * Returns a string representation of this vector.
     */
    public String toString()
    {
        String res = "[";
	boolean first = true;

	for( int i=0; i<elementCount; i++ ){
	    if( first ){
	        first = false;
	    }
	    else {
	        res += ",";
	    }
	    res += elementData[i];
	}

	return res + "]";
    }
}

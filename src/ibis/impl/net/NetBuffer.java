package ibis.ipl.impl.net;


/**
 * Provides a simple wrapper class for a byte array.
 *
 * The purpose of this class is to store a set of information together
 * and not to inforce any policy. As a result, each member and
 * attribute is public to indicate that no control is performed over
 * the consistency of the attribytes values by this class. In particular,
 * the methods are <strong>not</strong> thread safe. Note: this overall approach
 * might change in the future.
 */
public class NetBuffer {

	/**
	 * The byte array.
	 */
	public byte[] data = null;

	/**
	 * The length of the data stored in the buffer. We should always have
	 * <CODE>length <= {@link #data}.length</CODE>.
	 */
	public int length = 0;

	/**
	 * Stores an optional allocator reference.
	 *
	 * If the block was allocated through a {@link NetAllocator}, a reference to
	 * this allocator can be stored here by the contructor, so that the buffer
	 * may later be freed using it.
	 */
	public NetAllocator allocator = null;

	/**
	 * Constructor.
	 *
	 * @param data the buffer.
	 * @param length the length of the data stored in the buffer.
	 */
	public NetBuffer(byte[] data, int length) {
		this.data   = data;
		this.length = length;
	}

	/**
	 * Constructor.
	 *
	 * @param data the buffer.
	 * @param length the length of the data stored in the buffer.
	 * @param allocator the allocator used to allocate the buffer,
	 * or <CODE>null</CODE> if no allocator was used.
	 */
	public NetBuffer(byte[]       data,
			 int          length,
			 NetAllocator allocator) {
		this.data      = data;
		this.length    = length;
		this.allocator = allocator;
	}

	/**
	 * Frees the buffer. 
	 * 
	 * If an allocator reference is available in the
	 * {link #allocator} attribute, this allocator is used to free the buffer.
	 */
	public void free() {
		length = 0;
		if (allocator != null) {
			allocator.free(data);
			allocator = null;
		}

		data = null;
	}
}

package ibis.impl.net;


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
public class NetBuffer implements Cloneable {

	/**
	 * The byte array.
	 */
	public byte[] data = null;

	/**
	 * The length of the data stored in the buffer.
	 * This includes any headers.
	 */
	public int length = 0;

	/**
	 * If this is a larger contiguous buffer from which we seen ad lib,
	 * we need an offset field to indicate where to start.
	 */
	public int base = 0;

	/**
	 * Stores an optional allocator reference.
	 *
	 * If the block was allocated through a {@link NetAllocator}, a reference to
	 * this allocator can be stored here by the contructor, so that the buffer
	 * may later be freed using it.
	 */
	public NetAllocator allocator = null;

	/**
	 * Stores an optional factory reference.
	 *
	 * If the block was allocated through a {@link NetBufferFactory}, a reference to
	 * this factory can be stored here by the contructor, so that the buffer
	 * may later be freed using it.
	 */
	public NetBufferFactory factory = null;

	/**
	 * A factory may need a <CODE>next</CODE> pointer to maintain a linked
	 * list
	 */
	public NetBuffer next;

	/**
	 * Multiplexing drivers require that the buffer has information on
	 * the connection it wants to travel on.
	 */
	public Object connectionId;

	/**
	 * Indicates whether some layer has claimed ownership for this buffer.
	 * If so, other layers are not allowed to keep a reference to this
	 * buffer over calls; if they want to keep the buffer, they must clone
	 * it and keep the copy.
	 * If a leaf send call in the driver hierarchy gets the buffer with
	 * this field still cleared, it must free the buffer by invoking
	 * {@link #free}.
	 *
	 * Currently, this has yet only been given meaning for send buffers.
	 */
	public boolean ownershipClaimed;


	/**
	 * Constructor.
	 *
	 * @param data the buffer.
	 * @param length the length of the data stored in the buffer.
	 */
	public NetBuffer(byte[] data, int length) {
		this(data, 0, length, null);
	}

	/**
	 * Constructor.
         *
	 * @param data the buffer.
	 * @param length the length of the data stored in the buffer.
	 * @param allocator the allocator used to allocate the buffer,
	 * or <CODE>null</CODE> if no allocator was used.
	 */
	NetBuffer(byte[]       data,
		  int          length,
		  NetAllocator allocator) {
		this(data, 0, length, allocator);
	}

	/**
	 * Constructor.
         *
	 * @param data the buffer.
	 * @param base the offset into the buffer
	 * @param length the length of the data stored in the buffer.
	 */
	NetBuffer(byte[]       data,
		  int          base,
		  int          length) {
		this(data, base, length, null);
	}

	/**
	 * Constructor.
         *
	 * @param data the buffer.
	 * @param base the offset into the buffer
	 * @param length the length of the data stored in the buffer.
	 * @param allocator the allocator used to allocate the buffer,
	 * or <CODE>null</CODE> if no allocator was used.
	 */
	NetBuffer(byte[]       data,
		  int          base,
		  int          length,
		  NetAllocator allocator) {
		this.data      = data;
		this.base      = base;
		this.length    = length;
		this.allocator = allocator;
	}


	/**
	 * Resets all data for recycling after the packet has been freed.
	 */
	public void reset() {
	}


	/**
	 * Frees the buffer. 
	 * 
	 * If a factory was installed, this method can be used to cache or
	 * recycle the buffer.
	 * Else, if an allocator reference is available in the
	 * {link #allocator} attribute, this allocator is used to free the buffer.
	 * Otherwise this method is a no-op, and the GC will reclaim the buffer.
	 *
	 */
	public void free() {
		if (factory != null) {
		    factory.free(this);
		} else {
		    length = 0;
		    if (allocator != null) {
			    allocator.free(data);
			    allocator = null;
		    }

		    // Help the GC????
		    data = null;
		}
	}


	public NetBuffer makeCopy() {
	    NetBuffer copy = null;
	    try {
		copy = (NetBuffer)clone();
	    } catch (CloneNotSupportedException e) {
		throw new Error("What's up NOW: NetBuffer IS cloneable!");
	    }
	    return copy;
	}

}

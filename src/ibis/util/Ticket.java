package ibis.util;

/**
 * The <code>Ticket</code> class provides a mechanism that enables a user to
 * first obtain an identification number, give that identification number to
 * someone else, and then wait until that someone connects an object to that
 * identification number, and then collect that object. So, an "object" consumer
 * first calls <code>get()</code>. This gives him an identification ("ticket").
 * He gives this ticket to an "object" producer. This producer at some point
 * produces an object and calls <code>put(ticket, object)</code>. Meanwhile,
 * the consumer calls <code>collect(ticket)</code>, which will block until
 * an object has been connected to the ticket, and then return that object.
 */
public class Ticket { 
    /**
     * Enables/disables debugging prints.
     */
    private final static boolean DEBUG = false;

    /**
     * Initial number of tickets.
     */
    private final static int INIT_SIZE = 16;

    /**
     * Bucket associated with a ticket. It contains room for the object
     * that is to be associated with the ticket, and some other administrative
     * stuff. It also has methods to access those data.
     */
    private static class Bucket {
	/**
	 * Room for the object associated with a ticket.
	 */
	private Object	data;

	/**
	 * The number of threads waiting for something to happen on this bucket.
	 */
	private int		waiters;

	/**
	 * Flag indicating whether this bucket may currently be used (i.e., the
	 * corresponding ticket number has been given out).
	 */
	private boolean	valid;

	/**
	 * Flag indicating whether this bucket is currently initialized
	 * (i.e., a value has been put in it). A separate flag allows for
	 * null values as well.
	 */
	private boolean	initialized;

	/**
	 * Constructor.
	 */
	Bucket() {
	    data    = null;
	    waiters = 0;
	    valid	= false;
	    initialized = false;
	}

	/**
	 * Makes this bucket valid: its associated ticket number has been given out.
	 */
	synchronized void setValid() {
	    valid = true;
	}

	/**
	 * Gets and resets the object. It first blocks until a value is
	 * put(), and then grabs and destroys it.
	 * @return the object put into this bucket.
	 */
	synchronized Object get() {
	    Object result;

	    if (! valid) {
		throw new RuntimeException("Invalid ticket");
	    }
	    while (! initialized) {
		waiters++;
		try {
		    wait();
		} catch(InterruptedException e) {
		    // ignore
		}
		waiters--;
	    }

	    result = data;
	    initialized = false;

	    if (waiters != 0) {
		notifyAll();
	    }
	    return result;
	}

	/**
	 * Gets and resets the object. It first blocks until a value is
	 * put(), and then grabs and destroys it. The difference with
	 * <code>get</code> is that <code>collect</code> also makes the
	 * bucket invalid, so that a new ticket is required.
	 * @return the object put into this bucket.
	 */
	synchronized Object collect() {
	    Object result;

	    if (! valid) {
		throw new RuntimeException("Invalid ticket");
	    }
	    while (! initialized) {
		waiters++;
		try {
		    wait();
		} catch(InterruptedException e) {
		    // ignore
		}
		waiters--;
	    }

	    result = data;
	    initialized = false;
	    valid = false;
	    return result;
	}

	/**
	 * Releases this bucket. After this call, a new ticket is required.
	 */
	synchronized void release() {
	    if (! valid) {
		throw new RuntimeException("Invalid ticket");
	    }
	    initialized = false;
	    valid = false;
	}

	/**
	 * Gets the object. It first blocks until a value is put(), 
	 * and then returns it.
	 * @return the object put into this bucket.
	 */
	synchronized Object peek() {
	    if (! valid) {
		throw new RuntimeException("Invalid ticket");
	    }
	    while (! initialized) {
		waiters++;
		try {
		    wait();
		} catch(InterruptedException e) {
		    // ignore
		}
		waiters--;
	    }

	    return data;
	}

	/**
	 * Puts an object in the bucket. It first waits until the
	 * bucket is free, then puts the data in it, and notifies
	 * waiters.
	 * @param o the object to be placed in the bucket.
	 */
	synchronized void put(Object o) {
	    if (! valid) {
		throw new RuntimeException("Invalid ticket");
	    }
	    while (initialized) {
		waiters++;
		try {
		    wait();
		} catch(InterruptedException e) {
		    // ignore
		}
		waiters--;
	    }
	    data = o;
	    initialized = true;
	    if (waiters != 0) {
		notifyAll();
	    }
	}
    }

    /**
     * Number of tickets that we currently can handle.
     */
    private int size;

    /**
     * A stack of free ticket numbers.
     */
    private int [] tickets;

    /**
     * Top of the free ticket number stack. Ticket numbers are popped from this stack,
     * and pushed again when they become available for reuse.
     */
    private int top;

    private Bucket [] buckets;

    /**
     * Constructor. Creates the initial data structure for <code>INIT_SIZE</code> tickets.
     */
    public Ticket() { 
	this(INIT_SIZE);
    }

    /**
     * Constructor. Creates the initial data structure for <code>initialSize</code> tickets.
     *
     * @param initialSize the initial number of tickets.
     */
    public Ticket(int initialSize) { 
	buckets = new Bucket[initialSize];
	tickets = new int[initialSize];

	for (int i=0;i<initialSize;i++) { 
	    buckets[i] = new Bucket();
	    tickets[i] = i;
	}

	top  = initialSize;
	size = initialSize;

	if (DEBUG) { 
	    System.out.println("Ticket(" + initialSize + ") done");
	} 
    } 

    /**
     * Returns a new ticket. If not available, the data structure is doubled in size,
     * @return a new ticket number.
     */
    public synchronized int get() { 

	if (DEBUG) System.out.println("Ticket.get() starting");

	if (top == 0) { 

	    if (DEBUG) System.out.println("Ticket.get() resizing from " + size + " to " + (size*2));

	    // resize the lot.
	    int new_size = size*2;

	    // "tickets" is empty, so we can realloc it directly			
	    tickets = new int[new_size];

	    // buckets contains data, so copy it.
	    Bucket [] new_buckets = new Bucket[new_size];

	    System.arraycopy(buckets, 0, new_buckets, 0, size);

	    for (int i=0;i<size;i++) { 
		tickets[i] = size+i;
		new_buckets[size+i] = new Bucket();
	    }

	    top = size;
	    size = new_size;
	    buckets = new_buckets;
	} 

	top--;

	int ticket = tickets[top];

	buckets[ticket].setValid();

	if (DEBUG) System.out.println("Ticket.get() returning tickets[" + top + "] = " + ticket);

	return ticket;
    }

    /**
     * Associates <code>object</code> with <code>ticket</code> and notifies
     * anyone waiting on the corresponding lock. If an object is already associated
     * with this ticket, the method blocks until the ticket is made available
     * (by means of a <code>get(ticket)</code> call.
     *
     * @param ticket the ticket number that gets an object associated with it
     * @param object the object that gets associated
     */
    public void put(int ticket, Object object) { 
	Bucket bucket;

	if (DEBUG) System.out.println("Ticket.put(" + ticket + ") starting");

	synchronized(this) {
	    bucket = buckets[ticket];
	}

	if (DEBUG) System.out.println("Ticket.put() got a bucket");

	bucket.put(object);

	if (DEBUG) System.out.println("Ticket.put() done");
    } 

    /**
     * Returns the object that gets associated with <code>ticket</code>. The
     * <code>ticket</code> is made available for reuse.
     *
     * @param ticket the ticket number for which an object is now requested.
     * @return the object that got associated with <code>ticket</code>.
     */
    public Object collect(int ticket) {
	Bucket bucket;
	Object result;

	if (DEBUG) System.out.println("Ticket.collect(" + ticket + ") starting");

	synchronized(this) {
	    bucket = buckets[ticket];
	}

	if (DEBUG) System.out.println("Ticket.collect() got a bucket");

	result = bucket.collect();

	if (DEBUG) System.out.println("Ticket.collect() got a result");

	synchronized(this) {
	    tickets[top++] = ticket;
	}

	if (DEBUG) System.out.println("Ticket.collect() done");

	return result;
    }

    /**
     * Returns the object that gets associated with <code>ticket</code>.
     * This version is non-destructive: it leaves the associated value intact.
     *
     * @param ticket the ticket number for which an object is now requested.
     * @return the object that got associated with <code>ticket</code>.
     */
    public Object peek(int ticket) {
	Object result;
	Bucket bucket;

	if (DEBUG) System.out.println("Ticket.peek(" + ticket + ") starting");

	synchronized(this) {
	    bucket = buckets[ticket];
	}

	if (DEBUG) System.out.println("Ticket.peek() got a bucket");

	result = bucket.peek();

	if (DEBUG) System.out.println("Ticket.peek() done");

	return result;		
    }

    /**
     * Returns the object that gets associated with <code>ticket</code>.
     * This version is destructive (makes the ticket available for another
     * <code>put</code>), but does not release the ticket.
     * To release the ticket, <code>collect</code> must be used.
     *
     * @param ticket the ticket number for which an object is now requested.
     * @return the object that got associated with <code>ticket</code>.
     */
    public Object get(int ticket) {
	Object result;
	Bucket bucket;

	if (DEBUG) System.out.println("Ticket.get(" + ticket + ") starting");

	synchronized(this) {
	    bucket = buckets[ticket];
	}

	if (DEBUG) System.out.println("Ticket.get() got a bucket");

	result = bucket.get();

	if (DEBUG) System.out.println("Ticket.get() done");

	return result;		
    }

    /**
     * Releases <code>ticket</code>. 
     * This makes the ticket available for reuse.
     * @param ticket the ticket number to be released.
     */
    public void freeTicket(int ticket) {
	Bucket bucket;

	synchronized(this) {
	    bucket = buckets[ticket];
	}
	bucket.release();
	synchronized(this) {
	    tickets[top++] = ticket;
	}
    }
} 

package ibis.util;

public class Ticket { 

	/* I think there is still a race condition here !! 

	   The Ticket object lock should be always be locked when doing a 
	   get/put/collect to prevent resizing and retrieving at the same time.

	   Unfortunately, this won't work because we "wait/notify" on a seperate lock, which 
	   would mean we wouldn't release the Ticket object lock. 

          - Jason 
        */
		
	public final static boolean DEBUG = false;

	public final static int INIT_SIZE = 10;

	// number of tickets we can handle
	private int size;

	// a stack of tickets
	private int [] tickets;
	private int top;

	// the actual data we are interrested in. 
	private Object [] data;

	// an array of objects used as a lock.
	private Object [] locks;

	public Ticket() { 
		this(INIT_SIZE);
	}

	public Ticket(int initialSize) { 
		data    = new Object[initialSize];
		tickets = new int[initialSize];
		locks   = new Object[initialSize];
		
		for (int i=0;i<initialSize;i++) { 
			data[i]    = null;
			tickets[i] = i;
			locks[i]   = new Object();
		}
		
		top  = initialSize;
		size = initialSize;

		if (DEBUG) { 
			System.out.println("Ticket(" + initialSize + ") done");
		} 
	} 

	public synchronized int get() { 

		if (DEBUG) System.out.println("Ticket.get() starting");
		
		if (top == 0) { 

			if (DEBUG) System.out.println("Ticket.get() resizing from " + size + " to " + (size*2));

			// resize the lot.
			int new_size = size*2;
			
			// "tickets" is empty, so we can realloc it directly			
			int [] tickets = new int[new_size];
			
			// the others (may) contain data, so copy it.
			Object [] new_data = new Object[new_size];
			Object [] new_locks = new Object[new_size];

			System.arraycopy(data, 0, new_data, 0, size);
			System.arraycopy(locks, 0, new_locks, 0, size);
			
			for (int i=0;i<size;i++) { 
				tickets[i] = size+i;
				new_locks[size+i] = new Object();
			}
			
			top = size;
			size = new_size;
			data = new_data;
			locks = new_locks;
		} 

		top--;

		if (DEBUG) System.out.println("Ticket.get() returning tickets[" + top + "] = " + tickets[top]);

		return tickets[top];
	}
	
	public void put(int ticket, Object object) { 

		if (DEBUG) System.out.println("Ticket.put(" + ticket + ", " + object + ") starting");

		synchronized (this) { 
			if (DEBUG) System.out.println("Ticket.put() storing object");
			// synchronized to prevent a race with resize in get
			data[ticket] = object;
		}

		synchronized (locks[ticket]) { 
			if (DEBUG) System.out.println("Ticket.put() doing notifyAll()");
			locks[ticket].notifyAll();		
		}
	} 

	public Object collect(int ticket) {

		Object result;

		if (DEBUG) System.out.println("Ticket.collect(" + ticket + ") starting");

		synchronized (locks[ticket]) { 

			while (data[ticket] == null) { 

				if (DEBUG) System.out.println("Ticket.collect() waiting");

				try { 
					locks[ticket].wait();
				} catch (InterruptedException e) { 
					// ignore
				} 
			}			
		}

		synchronized (this) { 

			if (DEBUG) System.out.println("Ticket.collect() retrieving object");

			// synchronized to prevent a race with get
			result = data[ticket];
			data[ticket] = null;
			tickets[top++] = ticket;
		}
		
		if (DEBUG) System.out.println("Ticket.collect() done");

		return result;		
	}
} 

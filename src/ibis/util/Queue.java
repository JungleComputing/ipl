package ibis.util;

public class Queue {
	class QueueNode {
		QueueNode next;
		Object data;
	}

	QueueNode head, tail;
	int size;

	public synchronized void enqueue(Object o) {
		QueueNode node = new QueueNode();
		node.data = o;
		if(tail == null) {
			head = node;
		} else {
			tail.next = node;
		}

		tail = node;
		size++;

		notify();
	}

	public synchronized Object dequeue() {
		while(head == null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}

		QueueNode result = head;
		head = result.next;
		if(head == null) tail = null;
		size--;

		return result.data;
	}

	/**
	 * Dequeues the head of the queue. If the queue is empty it
	 * will wait until something is added to the queue, or the deadline
	 * has passed.
	 *
	 * @param deadline the deadline expressed in milliseconds 
	 *		   since 1-1-1970. a value of "0" will cause this
	 *		   function to wait forever, on -1 it will not wait 
	 *		   at all.
	 *
	 * @return the dequeued object, or null if the deadline passed
	 */
	public synchronized Object dequeue(long deadline) {
		while(head == null) {
			if (deadline == -1) {
				return null;
			} else if (deadline == 0) {
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			} else {
				long time = System.currentTimeMillis();

				if (time >= deadline) {
					return null;
				} else {
					try {
						wait(deadline - time);
					} catch (Exception e) {
						//IGNORE
					}
				}
			}
		}

		QueueNode result = head;
		head = result.next;
		if(head == null) tail = null;
		size--;

		return result.data;
	}

	public synchronized int size() {
		return size;
	}
}

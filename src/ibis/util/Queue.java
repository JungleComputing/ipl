package ibis.ipl.impl.generic;

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

		notifyAll();
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

	public int size() {
		return size;
	}
}

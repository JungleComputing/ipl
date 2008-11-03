package ibis.ipl.impl.stacking.lrmc.util;

public class BoundedObjectQueue {

    private final Object[] objects;
    private final int maxSize;

    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public BoundedObjectQueue(int size) {
        maxSize = size;
        objects = new Object[maxSize];
    }

    public synchronized void enqueue(Object o) {

        while (size == maxSize) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        objects[head] = o;
        head = (head + 1) % maxSize;
        size++;

        if (size == 1) {
            notifyAll();
        }
    }

    public synchronized Object dequeue() {

        while (size == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        Object tmp = objects[tail];
        objects[tail] = null;

        tail = (tail + 1) % maxSize;
        size--;

        if (size == maxSize - 1) {
            notifyAll();
        }

        return tmp;
    }
}

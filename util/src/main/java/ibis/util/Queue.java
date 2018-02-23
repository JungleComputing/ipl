/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.util;

/**
 * A simple queue class. Objects can be enqueued, dequeued, or dequeued
 * with a deadline.
 */
public class Queue {
    static class QueueNode {
        QueueNode next;

        Object data;
    }

    QueueNode head, tail;

    int size;

    /**
     * Constructs a new queue.
     */
    public Queue() {
        /* do nothing */
    }

    /**
     * Enqueues the specified object, and notifies: if there
     * are threads waiting, a single one is notified.
     * @param o the object to be enqueued.
     */
    public synchronized void enqueue(Object o) {
        QueueNode node = new QueueNode();
        node.data = o;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }

        tail = node;
        size++;

        notify();
    }

    /**
     * Dequeues the head of the queue. If the queue is empty it
     * will wait until something is added to the queue.
     *
     * @return the dequeued object.
     */
    public synchronized Object dequeue() {
        return dequeue(0L);
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
        while (head == null) {
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
                }
                try {
                    wait(deadline - time);
                } catch (Exception e) {
                    //IGNORE
                }
            }
        }

        QueueNode result = head;
        head = result.next;
        if (head == null) {
            tail = null;
        }
        size--;

        return result.data;
    }

    /**
     * Returns the length of the queue.
     * @return the length of the queue.
     */
    public synchronized int size() {
        return size;
    }
}

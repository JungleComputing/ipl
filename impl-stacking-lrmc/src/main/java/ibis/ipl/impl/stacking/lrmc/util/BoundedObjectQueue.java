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

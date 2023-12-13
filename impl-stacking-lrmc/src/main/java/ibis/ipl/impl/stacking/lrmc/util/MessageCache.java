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

public class MessageCache {

    private final int MESSAGE_SIZE;
    private final int MAX_SIZE;

    private Message cache;
    private int size;

    private long hits = 0;
    private long miss = 0;

    private long store = 0;
    private long discard = 0;

    public MessageCache(int cacheSize, int messageSize) {
        this.MAX_SIZE = cacheSize;
        this.MESSAGE_SIZE = messageSize;

        /*
         * // fill the cache for (int i=0; i<MAX_SIZE; i++) { Message m =
         * get(); put(m); }
         */
    }

    public synchronized void setDestinationSize(int count) {
        Message tmp = cache;
        while (tmp != null) {
            if (tmp.destinations == null || tmp.destinations.length < count) {
                tmp.destinations = new int[count];
            }

            tmp = tmp.next;
        }
    }

    public synchronized void put(Message m) {
        m.refcount--;
        if (m.refcount == 0) {
            if (size < MAX_SIZE && m.buffer != null
                    && m.buffer.length == MESSAGE_SIZE) {

                m.next = cache;
                cache = m;
                size++;
                store++;
            } else {
                m.next = null;
                discard++;
            }
        }
    }

    public Message get(int len) {
        if (len > MESSAGE_SIZE) {
            // System.err.println("Creating new message of size " + len);
            miss++;
            return new Message(len);
        }

        return get();
    }

    public synchronized Message get() {
        Message tmp = null;

        if (size == 0) {
            miss++;
            tmp = new Message(MESSAGE_SIZE);
        } else {
            hits++;

            tmp = cache;
            cache = cache.next;
            size--;
        }
        tmp.next = null;
        tmp.local = false;
        tmp.refcount = 1;

        return tmp;
    }

    public int getPrefferedMessageSize() {
        return MESSAGE_SIZE;
    }

    public void statistics() {
        System.out.println("hits: " + hits + ", miss = " + miss + ", store = " + store + ", discard = " + discard);
    }
}

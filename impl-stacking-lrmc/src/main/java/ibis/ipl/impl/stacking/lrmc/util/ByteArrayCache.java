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

import java.util.LinkedList;

public class ByteArrayCache {

    private static final int DEFAULT_MAX = 100;

    private LinkedList<byte[]> cache = new LinkedList<>();

    private final int maxSize;
    private final int arraySize;

    public ByteArrayCache(int arraySize) {
        this(arraySize, DEFAULT_MAX);
    }

    public ByteArrayCache(int arraySize, int maxArrays) {
        this.maxSize = maxArrays;
        this.arraySize = arraySize;
    }

    public synchronized void put(byte[] array) {
        if (cache.size() < maxSize && array.length == arraySize) {
            cache.addLast(array);
        }
    }

    public synchronized byte[] get(int len) {
        if (len <= arraySize && cache.size() > 0) {
            return cache.removeLast();
        }

        return new byte[len];
    }
}

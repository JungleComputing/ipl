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

package ibis.io;

public final class IbisVector {

    public static final int INIT_SIZE = 64;

    private static final int INCREMENT_FACTOR = 4;

    private Object[] array;

    private int current_size;

    private int maxfill;

    public IbisVector() {
        this(INIT_SIZE);
    }

    public IbisVector(int size) {
        array = new Object[size];
        current_size = size;
        maxfill = 0;
    }

    private final void double_array() {
        int new_size = current_size * INCREMENT_FACTOR;
        Object[] temp = new Object[new_size];
        // System.arraycopy(array, 0, temp, 0, current_size);
        System.arraycopy(array, 0, temp, 0, maxfill);
        array = temp;
        current_size = new_size;
    }

    public final void add(int index, Object data) {
        // System.err.println("objects.add: index = " + index + " data = "
        //         + (data == null ? "NULL" : data.getClass().getName()));

        while (index >= current_size) {
            double_array();
        }
        array[index] = data;
        if (index >= maxfill) {
            maxfill = index + 1;
        }
    }

    public final Object get(int index) {
        return array[index];
    }

    public final void clear() {
        for (int i = 0; i < maxfill; i++) {
            array[i] = null;
        }
        maxfill = 0;
    }
}

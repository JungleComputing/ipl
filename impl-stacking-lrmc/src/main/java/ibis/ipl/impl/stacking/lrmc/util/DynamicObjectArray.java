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

public class DynamicObjectArray<T> {

    private static final int DEFAULT_SIZE = 64;

    private Object[] objects;

    private int last = -1;

    public DynamicObjectArray() {
        this(DEFAULT_SIZE);
    }

    public DynamicObjectArray(int size) {
        objects = new Object[size];
    }

    private void resize(int minimumSize) {
        int newSize = objects.length;

        while (newSize <= minimumSize) {
            newSize *= 2;
        }

        Object[] tmp = new Object[newSize];
        System.arraycopy(objects, 0, tmp, 0, objects.length);
        objects = tmp;
    }

    public void put(int index, T o) {
        if (index >= objects.length) {
            resize(index);
        }

        objects[index] = o;

        if (index > last) {
            last = index;
        }
    }

    public void remove(int index) {
        /*
         * if (index > last) { System.err.println("illegal remove in
         * DynamicObjectArray"); return; }
         */

        objects[index] = null;
        if (index == last) {
            while (index >= 0 && objects[index] == null) {
                index--;
            }
            last = index;
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index > last) {
            return null;
        }

        return (T) objects[index];
    }

    public int last() {
        return last;
    }
}

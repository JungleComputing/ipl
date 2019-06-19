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
package ibis.ipl.benchmarks.concurrentSenders;

/* $Id$ */


import java.io.Serializable;

public final class DITree implements Serializable {

    private static final long serialVersionUID = -958642085796432906L;

    public static final int OBJECT_SIZE = 4 * 4 + 2 * 4;

    DITree left;

    DITree right;

    int i;

    int i1;

    int i2;

    int i3;

    public DITree(int size) {
        int leftSize = size / 2;
        if (leftSize > 0) {
            this.left = new DITree(leftSize);
        }
        if (size - leftSize - 1 > 0) {
            this.right = new DITree(size - leftSize - 1);
        }
    }
}


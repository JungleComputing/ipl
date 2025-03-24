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
package ibis.ipl.benchmarks.pingPong;

/* $Id$ */

import java.io.Serializable;

public final class Tree implements Serializable {

    private static final long serialVersionUID = -3021696985605964883L;

    public static final int PAYLOAD = 4 * 4;

    Tree left;
    Tree right;

    int i;
    int i1;
    int i2;
    int i3;

    public Tree(int size) {
        int leftSize = size / 2;
        if (leftSize > 0) {
            this.left = new Tree(leftSize);
        }
        if (size - leftSize - 1 > 0) {
            this.right = new Tree(size - leftSize - 1);
        }
    }
}

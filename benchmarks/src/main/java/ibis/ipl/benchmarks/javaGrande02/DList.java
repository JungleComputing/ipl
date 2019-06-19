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
package ibis.ipl.benchmarks.javaGrande02;

/* $Id$ */

import java.io.Serializable;

public final class DList implements Serializable {

    private static final long serialVersionUID = -1747815590000713983L;

    public static final int PAYLOAD = 4*4;

    DList next, prev;

    int i;
    int i1;
    int i2;
    int i3;

    private DList(int size, DList prev) { 
	if (size > 0) {
	    this.prev = prev;
	    this.next = new DList(size-1);
	}
    } 

    public DList(int size) {
	this.prev = null;
	this.next = new DList(size-1, this);
    }
}






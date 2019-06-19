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
package ibis.ipl.benchmarks.rpc;

/* $Id$ */



final class Data1 implements java.io.Serializable {

    private static final long serialVersionUID = 7366807900682527528L;

    static int fill;

    int i0;

    int i1;

    int i2;

    int i3;

    Data1() {
        i0 = fill++;
        i1 = fill++;
        i2 = fill++;
        i3 = fill++;
    }

}
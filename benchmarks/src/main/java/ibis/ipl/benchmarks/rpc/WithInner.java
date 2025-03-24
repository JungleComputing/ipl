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

class WithInner implements java.io.Serializable {

    private static final long serialVersionUID = -6251059778220583067L;

    static int count;

    class Inner implements java.io.Serializable {
        private static final long serialVersionUID = 6101579331738333255L;
        int x;

        Inner() {
            x = WithInner.this.x + 333;
        }
    }

    int x;

    Inner inner = new Inner();

    WithInner() {
        x = count++;
    }

    @Override
    public String toString() {
        return "x=" + x + "-inner.x=" + inner.x;
    }
}
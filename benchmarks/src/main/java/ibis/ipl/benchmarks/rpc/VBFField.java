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

import java.io.Serializable;

final public class VBFField implements Serializable {
    private static final long serialVersionUID = 6697247545896124734L;
    public int indexOfRemoteCopy;
    public double[] field = new double[3];
    transient public int indexOfSubDomain;

    public VBFField() {
    }

    public VBFField(int subDomIndex, int indexOfRemoteCopy, double[] field) {
        this.indexOfRemoteCopy = indexOfRemoteCopy;
        this.field = field;
        this.indexOfSubDomain = subDomIndex;
    }
}

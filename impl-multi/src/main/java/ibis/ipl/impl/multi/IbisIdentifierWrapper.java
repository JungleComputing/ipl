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
package ibis.ipl.impl.multi;

import ibis.ipl.IbisIdentifier;

final class IbisIdentifierWrapper implements Comparable<IbisIdentifierWrapper> {
    final IbisIdentifier id;
    final String ibisName;

    IbisIdentifierWrapper(String ibisName, IbisIdentifier id) {
        this.id = id;
        this.ibisName = ibisName;
    }

    @Override
    public int compareTo(IbisIdentifierWrapper w) {
        int compare = this.ibisName.compareTo(w.ibisName);
        if (compare == 0) {
            return id.compareTo(w.id);
        } else {
            return compare;
        }
    }
}

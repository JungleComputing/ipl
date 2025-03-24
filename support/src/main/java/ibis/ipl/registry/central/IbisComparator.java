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
package ibis.ipl.registry.central;

import java.util.Comparator;

import ibis.ipl.impl.IbisIdentifier;

/**
 * Compares two IbisIdentifiers made by this registry by numerically sorting
 * ID's
 *
 */
public class IbisComparator implements Comparator<IbisIdentifier> {

    @Override
    public int compare(IbisIdentifier one, IbisIdentifier other) {
        try {

            int oneID = Integer.parseInt(one.getID());
            int otherID = Integer.parseInt(other.getID());

            return oneID - otherID;

        } catch (NumberFormatException e) {
            // IGNORE
        }
        return one.getID().compareTo(other.getID());
    }

}

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
package ibis.ipl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import ibis.ipl.impl.IbisIdentifier;

public class Election {

    private final String name;
    private SortedSet<IbisIdentifier> candidates;

    Election(String name) {
        this.name = name;

        candidates = new TreeSet<>(new IbisComparator());
    }

    Election(DataInputStream in) throws IOException {
        name = in.readUTF();
        candidates = new TreeSet<>(new IbisComparator());

        int nrOfCandidates = in.readInt();

        if (nrOfCandidates < 0) {
            throw new IOException("negative candidate list value");
        }

        for (int i = 0; i < nrOfCandidates; i++) {
            candidates.add(new IbisIdentifier(in));
        }
    }

    synchronized void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeInt(candidates.size());
        for (IbisIdentifier candidate : candidates) {
            candidate.writeTo(out);
        }
    }

    synchronized void merge(Election other) {
        for (IbisIdentifier candidate : other.candidates) {
            candidates.add(candidate);
        }
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized int nrOfCandidates() {
        return candidates.size();
    }

    public synchronized IbisIdentifier getWinner() {
        if (candidates.isEmpty()) {
            return null;
        }

        // use sorting function of set to determine winner
        return candidates.first();
    }

    public synchronized IbisIdentifier[] getCandidates() {
        if (candidates.isEmpty()) {
            return new IbisIdentifier[0];
        }

        // use sorting function of set to determine winner
        return candidates.toArray(new IbisIdentifier[0]);
    }

    public synchronized void addCandidate(IbisIdentifier candidate) {
        candidates.add(candidate);
    }

    @Override
    public synchronized String toString() {
        String result = name + " candidates: ";

        for (IbisIdentifier candidate : candidates) {
            result += candidate;
        }

        return result;
    }

}

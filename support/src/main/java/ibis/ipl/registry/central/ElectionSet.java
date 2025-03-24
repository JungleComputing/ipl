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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ibis.ipl.impl.IbisIdentifier;

public class ElectionSet implements Iterable<Election> {

    private final Map<String, Election> elections;

    public ElectionSet() {
        elections = new HashMap<>();
    }

    public void init(DataInput in) throws IOException {
        int nrOfElections = in.readInt();

        if (nrOfElections < 0) {
            throw new IOException("negative number of events");
        }

        for (int i = 0; i < nrOfElections; i++) {
            Event event = new Event(in);
            Election election = new Election(event);
            elections.put(election.getName(), election);
        }
    }

    public void writeTo(DataOutput out) throws IOException {
        out.writeInt(size());

        for (Election election : elections.values()) {
            election.getEvent().writeTo(out);
        }
    }

    public int size() {
        return elections.size();
    }

    public void put(Election election) {
        elections.put(election.getName(), election);
    }

    public Election get(String electionName) {
        return elections.get(electionName);
    }

    public void remove(String electionName) {
        elections.remove(electionName);
    }

    public Election[] getElectionsWonBy(IbisIdentifier identifier) {
        ArrayList<Election> result = new ArrayList<>();

        for (Election election : elections.values()) {
            if (election.getWinner().equals(identifier)) {
                result.add(election);
            }
        }
        return result.toArray(new Election[0]);
    }

    public List<Event> getEvents() {
        ArrayList<Event> result = new ArrayList<>();

        for (Election election : elections.values()) {
            result.add(election.getEvent());
        }

        return result;
    }

    @Override
    public Iterator<Election> iterator() {
        return elections.values().iterator();
    }

}

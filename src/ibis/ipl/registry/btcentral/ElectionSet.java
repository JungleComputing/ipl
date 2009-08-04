package ibis.ipl.registry.btcentral;

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ElectionSet implements Iterable<Election> {

    private final Map<String, Election> elections;

    public ElectionSet() {
        elections = new HashMap<String, Election>();
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
        ArrayList<Election> result = new ArrayList<Election>();

        for (Election election : elections.values()) {
            if (election.getWinner().equals(identifier)) {
                result.add(election);
            }
        }
        return result.toArray(new Election[0]);
    }

    public List<Event> getEvents() {
        ArrayList<Event> result = new ArrayList<Event>();

        for (Election election : elections.values()) {
            result.add(election.getEvent());
        }

        return result;
    }

    public Iterator<Election> iterator() {
        return elections.values().iterator();
    }

}

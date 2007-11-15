package ibis.ipl.impl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class ElectionSet {
    
    private final TypedProperties properties;
    private final Registry registry;
    
    private final HashMap<String, Election> elections;

    public ElectionSet(TypedProperties properties, Registry registry) {
        this.properties = properties;
        this.registry = registry;
        
        elections = new HashMap<String, Election>();
    }

    public synchronized void writeGossipData(DataOutputStream out) throws IOException {
        out.writeInt(elections.size());
        
        for(Election election: elections.values()) {
            election.writeTo(out);
        }
    }

    public void readGossipData(DataInputStream in) throws IOException {
        int nrOfElections = in.readInt();
        
        if (nrOfElections < 0) {
            throw new IOException("negative election list size");
        }
        
        for (int i = 0; i < nrOfElections; i++) {
            Election election = new Election(in);
            String name = election.getName();
            
            if (elections.containsKey(name)) {
                elections.get(name).merge(election);
            } else {
                elections.put(name, election);
            }
        }
    }

    public synchronized IbisIdentifier getElectionResult(String electionName, long timeoutMillis) {
        try {
            wait(timeoutMillis);
        } catch (InterruptedException e) {
            //IGNORE
        }
        
        Election election = elections.get(electionName);
        
        if (election == null) {
            return null;
        }
        
        return election.getWinner();
    }

    public synchronized IbisIdentifier elect(String electionName, long timeoutMillis) {
        Election election = elections.get(electionName);
        
        if (election == null) {
            election = new Election(electionName);
        }
        
        if (election.nrOfCandidates() == 0) {
            election.addCandidate(registry.getIbisIdentifier());
        }

        try {
            wait(timeoutMillis);
        } catch (InterruptedException e) {
            //IGNORE
        }

        //re-fetch election (just in case it got removed while we were waiting)
        election = elections.get(electionName);
        
        if (election == null) {
            election = new Election(electionName);
        }
        
        if (election.nrOfCandidates() == 0) {
            election.addCandidate(registry.getIbisIdentifier());
        }        
        return election.getWinner();
    }

    public IbisIdentifier elect(String electionName) {
        return elect(electionName, properties.getIntProperty(RegistryProperties.ELECTION_TIMEOUT) * 1000);
    }

}

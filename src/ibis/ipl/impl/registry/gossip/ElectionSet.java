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

    public synchronized IbisIdentifier elect(String electionName) {
        // TODO Auto-generated method stub
        return null;
    }

    public IbisIdentifier elect(String electionName, long timeoutMillis) {
        // TODO Auto-generated method stub
        return null;
    }

}

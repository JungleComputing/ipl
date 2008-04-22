package ibis.ipl.impl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class Election {
    
    private final String name;
    private SortedSet<IbisIdentifier> candidates;
    
    Election(String name) {
        this.name = name;
        
        candidates = new TreeSet<IbisIdentifier>(new IbisComparator());
    }
    
    Election(DataInputStream in) throws IOException {
        name = in.readUTF();
        candidates = new TreeSet<IbisIdentifier>(new IbisComparator());
        
        int nrOfCandidates = in.readInt();
        
        if (nrOfCandidates < 0) {
            throw new IOException("negative candidate list value");
        }
        
        for(int i = 0; i < nrOfCandidates; i++) {
            candidates.add(new IbisIdentifier(in));
        }
    }
    
    synchronized void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeInt(candidates.size());
        for(IbisIdentifier candidate: candidates) {
            candidate.writeTo(out);
        }
    }

    synchronized void merge(Election other) {
        for(IbisIdentifier candidate: other.candidates) {
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
        
        //use sorting function of set to determine winner
        return candidates.first();
    }
    
    public synchronized IbisIdentifier[] getCandidates() {
        if (candidates.isEmpty()) {
            return new IbisIdentifier[0];
        }
        
        //use sorting function of set to determine winner
        return candidates.toArray(new IbisIdentifier[0]);
    }
    
    public synchronized void addCandidate(IbisIdentifier candidate) {
        candidates.add(candidate);
    }
    
    public synchronized String toString() {
        String result = name + " candidates: ";
        
        for(IbisIdentifier candidate: candidates) {
            result += candidate;
        }
        
        return result;
    }
    
    
    
}

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
    
    Election(String name, IbisIdentifier winner) {
        this.name = name;
        
        candidates = new TreeSet<IbisIdentifier>(new IbisComparator());
    }
    
    Election(DataInputStream in) throws IOException {
        name = in.readUTF();
        
        int nrOfCandidates = in.readInt();
        
        if (nrOfCandidates < 0) {
            throw new IOException("negative candidate list size");
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

    public synchronized IbisIdentifier getWinner() {
        //use sorting function of set to determine winner
        return candidates.first();
    }
    
    
    
}

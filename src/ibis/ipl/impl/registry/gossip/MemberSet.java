package ibis.ipl.impl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

class MemberSet {

    MemberSet(TypedProperties properties, Registry registry) {

    }

    public Member getMember(IbisIdentifier ibis) {
        // TODO Auto-generated method stub
        return null;
    }

    public void maybeDead(IbisIdentifier suspect) {
        // TODO Auto-generated method stub
        
    }

    public void assumeDead(IbisIdentifier deceased) {
        // TODO Auto-generated method stub
        
    }

    public void leave(IbisIdentifier ibis) {
        
    }

    public void writeGossipData(DataOutputStream stream) {
        // TODO Auto-generated method stub
        
    }
    
    public void readGossipData(DataInputStream stream) {
        // TODO Auto-generated method stub
        
    }


}

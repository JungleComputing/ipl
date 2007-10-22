package ibis.ipl.impl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;

import java.util.ArrayList;

class Member {
    
    private IbisIdentifier identifier;
    
    private long lastSeen;
    
    //ibisses who claim this member is no more
    //cleared upon succesful contact
    private ArrayList<IbisIdentifier> witnesses;
    
    private boolean dead;
    private boolean left;
    
    Member(IbisIdentifier identifier) {
        this.identifier = identifier;
        
        lastSeen = System.currentTimeMillis();
        
        witnesses = new ArrayList<IbisIdentifier>();
        
        dead = false;
        left = false;
    }
    
    synchronized void dead() {
        dead = true;
    }
    
    synchronized void left() {
        left = true;
    }
    
    synchronized boolean isDead() {
        return dead;
    }
    
    synchronized boolean hasLeft() {
        return left;
    }

}

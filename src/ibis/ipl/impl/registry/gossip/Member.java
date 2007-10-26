package ibis.ipl.impl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

class Member {

    private static final Logger logger = Logger.getLogger(Member.class);

    private final IbisIdentifier identifier;

    private final TypedProperties properties;
    
    private long lastSeen;

    // ibisses who claim this member is no more
    // cleared upon succesful contact
    private Set<IbisIdentifier> witnesses;

    
    // member can be declared dead after the timeout has expired AND at least
    // N witnesses have not been able to contact the peer.
    // witnesses and timeout cleared whenever a peer is able to contact the node
    private boolean dead;

    private boolean left;

    Member(IbisIdentifier identifier, TypedProperties properties) {
        this.properties = properties;

        this.identifier = identifier;

        lastSeen = System.currentTimeMillis();

        witnesses = new HashSet<IbisIdentifier>();

        dead = false;
        left = false;
    }
    
    Member(DataInputStream in, TypedProperties properties) throws IOException {
        this.properties = properties;
        
        identifier = new IbisIdentifier(in);
        
        //assume there was no transfer time. Good enough
        //for this timeout
        long notSeenFor = in.readLong();
        lastSeen = System.currentTimeMillis() - notSeenFor;

        int nrOfWittnesses = in.readInt();
        
        if (nrOfWittnesses < 0) {
            throw new IOException("negative list size");
        }
        
        witnesses = new HashSet<IbisIdentifier>();
        
        for (int i = 0; i < nrOfWittnesses; i++) {
            witnesses.add(new IbisIdentifier(in));
        }
        
        dead = in.readBoolean();
        left = in.readBoolean();
        
    }
    
    void writeTo(DataOutputStream out) throws IOException {
        identifier.writeTo(out);

        //send value independant of our clock
        long notSeenFor = System.currentTimeMillis() - lastSeen;
        out.writeLong(notSeenFor);
        
        out.writeInt(witnesses.size());
        for(IbisIdentifier witness: witnesses) {
            witness.writeTo(out);
        }
        
        out.writeBoolean(dead);
        out.writeBoolean(left);
    }
    
    IbisIdentifier getIdentifier() {
        return identifier;
    }

    synchronized void setLeft() {
        left = true;
    }

    public synchronized void declareDead() {
        dead = true;
        
        witnesses.clear();
    }
    
    synchronized boolean isDead() {
        return dead;
    }

    synchronized boolean hasLeft() {
        return left;
    }

    synchronized boolean timedout() {
        long timeout = properties.getLongProperty(RegistryProperties.PEER_DEAD_TIMEOUT);
        
        return System.currentTimeMillis() > (lastSeen + timeout);
    }

    synchronized void seen() {
        if (dead) {
            logger.warn("contacted dead member: " + this);
            return;
        }

        lastSeen = System.currentTimeMillis();

        witnesses.clear();
    }

    synchronized void addWitness(IbisIdentifier witness) {
        if (dead) {
            // already dead
            return;
        }

        witnesses.add(witness);

        int witnessesRequired = properties.getIntProperty(RegistryProperties.WITNESSES_REQUIRED);
        
        if (timedout() && witnesses.size() > witnessesRequired) {
            dead = true;
            witnesses.clear();
        }

    }

    public synchronized String toString() {
        double age = (double) (System.currentTimeMillis() - lastSeen) / 1000.0;

        return String.format("%s last seen %.2f seconds ago, witnesses: %d",
                identifier, age, witnesses.size());
    }

}

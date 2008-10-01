package ibis.ipl.impl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Member {

    private static final Logger logger = LoggerFactory.getLogger(Member.class);

    private final IbisIdentifier identifier;

    private final TypedProperties properties;
    
    private long lastSeen;

    // ibisses who claim this member is no more
    // cleared upon succesful contact
    private Set<UUID> witnesses;
    
    // member can be declared dead after the timeout has expired AND at least
    // N witnesses have not been able to contact the peer.
    // witnesses and timeout cleared whenever a peer is able to contact the node
    private boolean dead;

    private boolean left;

    Member(IbisIdentifier identifier, TypedProperties properties) {
        this.properties = properties;

        this.identifier = identifier;

        lastSeen = System.currentTimeMillis();

        witnesses = new HashSet<UUID>();

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
            throw new IOException("negative list value");
        }
        
        witnesses = new HashSet<UUID>();
        
        for (int i = 0; i < nrOfWittnesses; i++) {
            witnesses.add(new UUID(in.readLong(), in.readLong()));
        }
        
        dead = in.readBoolean();
        left = in.readBoolean();
        
    }
    
    synchronized void writeTo(DataOutputStream out) throws IOException {
        identifier.writeTo(out);

        //send value independant of our clock
        long notSeenFor = System.currentTimeMillis() - lastSeen;
        out.writeLong(notSeenFor);
        
        out.writeInt(witnesses.size());
        for(UUID witness: witnesses) {
            out.writeLong(witness.getMostSignificantBits());
            out.writeLong(witness.getLeastSignificantBits());
        }
        
        out.writeBoolean(dead);
        out.writeBoolean(left);
    }
    
    synchronized public void merge(Member other) {
        logger.debug("merging " + this + " with " + other);
        
        if (other.lastSeen > lastSeen) {
            lastSeen = other.lastSeen;
        }
        
        for(UUID witness: other.witnesses) {
            witnesses.add(witness);
        }
        
        if (other.dead) {
            dead = true;
        }
        
        if (other.left) {
            left = true;
        }
        
        if (dead || left) {
            witnesses.clear();
        }
        
        //check if this member is now dead
        int witnessesRequired = properties.getIntProperty(RegistryProperties.WITNESSES_REQUIRED);
        
        
        
        if (timedout() && witnesses.size() > witnessesRequired) {
            logger.debug("member dead after merge: " + witnesses.size());
            
            dead = true;
            witnesses.clear();
        }
        
        logger.debug("merge result = " + this);
    }
    
    synchronized IbisIdentifier getIdentifier() {
        return identifier;
    }

    synchronized UUID getUUID() {
        return UUID.fromString(identifier.getID());
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
        long timeout = properties.getIntProperty(RegistryProperties.PEER_DEAD_TIMEOUT) * 1000;
        
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

    synchronized void suspectDead(IbisIdentifier witness) {
        if (dead) {
            // already dead
            return;
        }

        witnesses.add(UUID.fromString(witness.getID()));

        int witnessesRequired = properties.getIntProperty(RegistryProperties.WITNESSES_REQUIRED);
        
        if (timedout() && witnesses.size() > witnessesRequired) {
            dead = true;
            witnesses.clear();
        }

    }
    
    synchronized boolean isSuspect() {
        if (dead || left) {
            return false;
        }
        
        return timedout();
    }

    public synchronized String toString() {
        long age = System.currentTimeMillis() - lastSeen;

        return String.format("%s last seen %d milliseconds ago, witnesses: %d, timeout = %b, left = %b, dead = %b",
                identifier, age, witnesses.size(), timedout(), hasLeft(), isDead());
    }

    public synchronized int nrOfWitnesses() {
        return witnesses.size();
    }


}

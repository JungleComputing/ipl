package ibis.ipl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.registry.statistics.Statistics;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MemberSet extends Thread {

    private static final Logger logger = LoggerFactory
            .getLogger(MemberSet.class);

    private final TypedProperties properties;

    private final Registry registry;

    private final Statistics statistics;

    private final HashSet<UUID> deceased;

    private final HashSet<UUID> left;

    private final HashMap<UUID, Member> members;

    private Member self;

    private final Random random;

    /**
     * Members that are actually reachable.
     */
    private int liveMembers;

    MemberSet(TypedProperties properties, Registry registry,
            Statistics statistics) {
        this.properties = properties;
        this.registry = registry;
        this.statistics = statistics;

        deceased = new HashSet<UUID>();
        left = new HashSet<UUID>();
        members = new HashMap<UUID, Member>();

        random = new Random();
    }

    @Override
    public synchronized void start() {
        this.setDaemon(true);

        super.start();

    }

    private synchronized Member getMember(IbisIdentifier ibis, boolean create) {
        Member result;

        UUID id = UUID.fromString(ibis.getID());

        if (deceased.contains(id) || left.contains(id)) {
            return null;
        }

        result = members.get(id);

        if (result == null && create) {
            result = new Member(ibis, properties);
            members.put(id, result);
            registry.ibisJoined(ibis);
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
        }

        return result;
    }

    public synchronized void maybeDead(IbisIdentifier ibis) {
        Member member = getMember(ibis, false);

        if (member == null) {
            return;
        }

        member.suspectDead(registry.getIbisIdentifier());

        cleanup(member);
    }

    public synchronized void assumeDead(IbisIdentifier ibis) {
        Member member = getMember(ibis, false);

        if (member == null) {
            return;
        }

        member.declareDead();

        cleanup(member);
    }

    public synchronized void leave(IbisIdentifier ibis) {
        Member member = getMember(ibis, true);

        if (member == null) {
            return;
        }

        member.setLeft();

        cleanup(member);
    }

    public synchronized void leave() {
        if (self != null) {
            self.setLeft();
        }
    }

    public synchronized IbisIdentifier getFirstLiving(
            IbisIdentifier[] candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }

        for (IbisIdentifier candidate : candidates) {
            Member member = getMember(candidate, false);

            if (member != null && !member.hasLeft() && !member.isDead()) {
                return candidate;
            }
        }

        // no alive canidates found, return first candidate
        return candidates[0];
    }

    public void writeGossipData(DataOutputStream out, int gossipSize) throws IOException {
        UUID[] deceased;
        UUID[] left;
        Member[] randomMembers;

        synchronized (this) {
            deceased = this.deceased.toArray(new UUID[0]);
            left = this.left.toArray(new UUID[0]);
            randomMembers = getRandomMembers(gossipSize);
            if (self != null) {
                // make sure we send out ourselves as "just seen"
                self.seen();
            }
        }

        out.writeInt(deceased.length);
        for (UUID id : deceased) {
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());
        }

        out.writeInt(left.length);
        for (UUID id : left) {
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());
        }

        out.writeInt(randomMembers.length);
        for (Member member : randomMembers) {
            member.writeTo(out);
        }
    }

    public void readGossipData(DataInputStream in) throws IOException {
        int nrOfDeceased = in.readInt();

        if (nrOfDeceased < 0) {
            throw new IOException("negative deceased list value");
        }

        ArrayList<UUID> newDeceased = new ArrayList<UUID>();
        for (int i = 0; i < nrOfDeceased; i++) {
            UUID id = new UUID(in.readLong(), in.readLong());
            newDeceased.add(id);
        }

        int nrOfLeft = in.readInt();

        if (nrOfLeft < 0) {
            throw new IOException("negative left list value");
        }

        ArrayList<UUID> newLeft = new ArrayList<UUID>();
        for (int i = 0; i < nrOfLeft; i++) {
            UUID id = new UUID(in.readLong(), in.readLong());
            newLeft.add(id);
        }

        int nrOfMembers = in.readInt();

        if (nrOfMembers < 0) {
            throw new IOException("negative member list value");
        }

        ArrayList<Member> newMembers = new ArrayList<Member>();
        for (int i = 0; i < nrOfMembers; i++) {
            Member member = new Member(in, properties);
            newMembers.add(member);
        }

        synchronized (this) {
            for (Member member : newMembers) {
                UUID id = member.getUUID();

                if (members.containsKey(id)) {
                    // merge state of know and received member
                    members.get(id).merge(member);
                } else if (!deceased.contains(id) && !left.contains(id)) {
                    // add new member
                    members.put(id, member);
                    // tell registry about his new member
                    registry.ibisJoined(member.getIdentifier());
                    if (statistics != null) {
                        statistics.newPoolSize(members.size());
                    }
                }
            }

            for (UUID id : newDeceased) {
                if (members.containsKey(id)) {
                    members.get(id).declareDead();
                } else if (!left.contains(id)) {
                    deceased.add(id);
                }
            }

            for (UUID id : newLeft) {
                if (members.containsKey(id)) {
                    members.get(id).setLeft();
                } else {
                    left.add(id);
                }
            }
        }
    }

    /**
     * Clean up the list of members. Also passes leave and died events to the
     * registry.
     */
    private synchronized void cleanup(Member member) {
        if (deceased.contains(member.getUUID())) {
            member.declareDead();
        }

        if (left.contains(member.getUUID())) {
            member.setLeft();
        }

        // if there are not enough live members in a pool to reach the
        // minimum needed to otherwise declare a member dead, do it now
        if (member.isSuspect() && member.nrOfWitnesses() >= liveMembers) {
            if (logger.isWarnEnabled()) {
        	logger.warn("declared " + member + " with "
        		+ member.nrOfWitnesses()
        		+ " witnesses dead due to a low number of live members ("
        		+ liveMembers + ").");
            }
            member.declareDead();
        }

        if (member.hasLeft()) {
            left.add(member.getUUID());
            members.remove(member.getUUID());
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            registry.ibisLeft(member.getIdentifier());
            if (logger.isDebugEnabled()) {
        	logger.debug("purged " + member + " from list");
            }
        } else if (member.isDead()) {
            deceased.add(member.getUUID());
            members.remove(member.getUUID());
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            registry.ibisDied(member.getIdentifier());
            if (logger.isDebugEnabled()) {
        	logger.debug("purged " + member + " from list");
            }
        }
    }

    /**
     * Update number of "alive" members
     */
    private synchronized void updateLiveMembers() {
        int result = 0;
        for (Member member : members.values()) {
            if (!member.isDead() && !member.hasLeft() && !member.timedout()) {
                result++;
            }
        }
        liveMembers = result;
    }

    /**
     * Clean up the list of members.
     */
    private synchronized void cleanup() {
        // notice ourselves ;)
        if (self != null) {
            self.seen();
        }

        // update live member count
        updateLiveMembers();

        // iterate over copy of values, so we can remove them if we need to
        for (Member member : members.values().toArray(new Member[0])) {
            cleanup(member);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(self.getIdentifier() + ": members = " + members.size()
                    + ", left = " + left.size() + " deceased = "
                    + deceased.size());
        }
    }

    private synchronized Member[] getRandomSuspects(int count) {
        ArrayList<Member> suspects = new ArrayList<Member>();

        for (Member member : members.values()) {
            if (member.isSuspect()) {
                suspects.add(member);
            }
        }

        while (suspects.size() > count) {
            suspects.remove(random.nextInt(suspects.size()));
        }

        return suspects.toArray(new Member[0]);
    }
    
    synchronized Member[] getRandomMembers(int count) {
	if (count < 0) {
	    return new Member[0];
	}

        ArrayList<Member> result = new ArrayList<Member>(members.values());

        while (result.size() > count) {
	    result.remove(random.nextInt(result.size()));
        }

        return result.toArray(new Member[0]);
    }

    synchronized void printMembers() {
        System.out.println("pool at " + registry.getIbisIdentifier());
        System.out.println("dead:");
        for (UUID member : deceased) {
            System.out.println(member);
        }
        System.out.println("left:");
        for (UUID member : left) {
            System.out.println(member);
        }
        System.out.println("current:");
        for (Member member : members.values()) {
            System.out.println(member);
        }
    }

    // ping suspect members once a second
    public void run() {
        Member self;
        synchronized (this) {
            // add ourselves to the member list
            self = new Member(registry.getIbisIdentifier(), properties);
            this.self = self;
            members.put(self.getUUID(), self);
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            registry.ibisJoined(self.getIdentifier());
        }

        long interval = properties
                .getIntProperty(RegistryProperties.PING_INTERVAL) * 1000;
        int count = properties.getIntProperty(RegistryProperties.PING_COUNT);

        while (!registry.isStopped()) {
            cleanup();

            Member[] suspects = getRandomSuspects(count);

            if (logger.isDebugEnabled()) {
        	logger.debug(self.getIdentifier() + ": checking " + suspects.length
        		+ " suspects");
            }

            for (Member suspect : suspects) {
                if (suspect.equals(self)) {
                    logger.error("we are a suspect ourselves");
                    suspect.seen();
                } else {
                    if (logger.isDebugEnabled()) {
                	logger.debug("suspecting " + suspect
                		+ " is dead, checking");
                    }
                    try {
                        registry.getCommHandler().ping(suspect.getIdentifier());
                        suspect.seen();
                    } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                	    logger.debug("could not reach " + suspect
                		    + ", adding ourselves as witness");
                	}
                        suspect.suspectDead(registry.getIbisIdentifier());
                    }
                    if (logger.isDebugEnabled()) {
                	logger.debug("done checking " + suspect);
                    }

                }
            }

            if (logger.isDebugEnabled()) {
        	logger.debug(self.getIdentifier() + ": done checking "
        		+ suspects.length + " suspects");
            }

            int timeout = (int) (Math.random() * interval);
            synchronized (this) {
                if (timeout > 0) {
                    try {
                        wait(timeout);
                    } catch (InterruptedException e) {
                        // IGNORE
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(self.getIdentifier() + ": registry stopped, exiting");
        }
    }
}

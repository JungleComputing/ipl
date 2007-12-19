package ibis.ipl.impl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.statistics.Statistics;
import ibis.util.TypedProperties;

class Pool extends Thread {

    private static final Logger logger = Logger.getLogger(Pool.class);

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

    Pool(TypedProperties properties, Registry registry, Statistics statistics) {
        this.properties = properties;
        this.registry = registry;
        this.statistics = statistics;

        deceased = new HashSet<UUID>();
        left = new HashSet<UUID>();
        members = new HashMap<UUID, Member>();

        // add ourselves to the list of members

        random = new Random();

    }

    @Override
    public synchronized void start() {
        this.setDaemon(true);

        super.start();

    }

    private synchronized Member getMember(IbisIdentifier ibis) {
        Member result;

        UUID id = UUID.fromString(ibis.getID());

        if (deceased.contains(id) || left.contains(id)) {
            return null;
        }

        result = members.get(id);

        if (result == null) {
            result = new Member(ibis, properties);
            members.put(id, result);
            registry.ibisJoined(ibis);
            statistics.newPoolSize(members.size());
        }

        return result;
    }

    public synchronized void maybeDead(IbisIdentifier ibis) {
        Member member = getMember(ibis);

        if (member == null) {
            return;
        }

        member.suspectDead(registry.getIbisIdentifier());

        cleanup(member);
    }

    public synchronized void assumeDead(IbisIdentifier ibis) {
        Member member = getMember(ibis);

        if (member == null) {
            return;
        }

        member.declareDead();

        cleanup(member);
    }

    public synchronized void leave(IbisIdentifier ibis) {
        Member member = getMember(ibis);

        if (member == null) {
            return;
        }

        member.setLeft();

        cleanup(member);
    }

    public synchronized void writeGossipData(DataOutputStream out)
            throws IOException {
        out.writeInt(deceased.size());
        for (UUID id : deceased) {
            out.writeLong(id.getLeastSignificantBits());
            out.writeLong(id.getMostSignificantBits());
        }

        out.writeInt(left.size());
        for (UUID id : left) {
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());
        }

        out.writeInt(members.size());
        for (Member member : members.values()) {
            member.writeTo(out);
        }
    }

    public synchronized void readGossipData(DataInputStream in)
            throws IOException {
        int nrOfDeceased = in.readInt();

        if (nrOfDeceased < 0) {
            throw new IOException("negative deceased list value");
        }

        for (int i = 0; i < nrOfDeceased; i++) {
            UUID id = new UUID(in.readLong(), in.readLong());
            deceased.add(id);
        }

        int nrOfLeft = in.readInt();

        if (nrOfLeft < 0) {
            throw new IOException("negative left list value");
        }

        for (int i = 0; i < nrOfLeft; i++) {
            UUID id = new UUID(in.readLong(), in.readLong());
            left.add(id);
        }

        int nrOfMembers = in.readInt();

        if (nrOfMembers < 0) {
            throw new IOException("negative member list value");
        }

        for (int i = 0; i < nrOfMembers; i++) {
            Member member = new Member(in, properties);
            UUID id = member.getUUID();

            if (members.containsKey(id)) {
                // merge state of know and received member
                members.get(id).merge(member);
            } else if (!deceased.contains(id) && !left.contains(id)) {
                // add new member
                members.put(id, member);
                // tell registry about his new member
                registry.ibisJoined(member.getIdentifier());
                statistics.newPoolSize(members.size());
            }
        }
    }

    /**
     * Clean up the list of members. Also passes leave and died events to the
     * registry.
     */
    private synchronized void cleanup(Member member) {
        // if there are not enough live members in a pool to reach the
        // minimum needed to
        if (member.isSuspect() && member.nrOfWitnesses() >= liveMembers) {
            logger.debug("declared " + member + " with "
                    + member.nrOfWitnesses()
                    + " witnesses dead due to a low number of live members ("
                    + liveMembers + ").");
            member.declareDead();
        }

        if (member.hasLeft()) {
            left.add(member.getUUID());
            registry.ibisLeft(member.getIdentifier());
            members.remove(member.getUUID());
            statistics.newPoolSize(members.size());
            logger.debug("purged " + member + " from list");
        } else if (member.isDead()) {
            String id = member.getIdentifier().getID();
            deceased.add(member.getUUID());
            members.remove(id);
            statistics.newPoolSize(members.size());
            registry.ibisDied(member.getIdentifier());
        }
    }

    /**
     * Update number of "alive" members
     */
    private synchronized void updateLiveMembers() {
        int result = 0;
        for (Member member : members.values()) {
            if (!member.isDead() && !member.hasLeft() && !member.isSuspect()) {
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
        self.seen();

        // update live member count
        updateLiveMembers();

        // iterate over copy of values, so we can remove them if we need to
        for (Member member : members.values().toArray(new Member[0])) {
            cleanup(member);
        }
    }

    private synchronized Member getSuspect() {
        ArrayList<Member> suspects = new ArrayList<Member>();

        for (Member member : members.values()) {
            if (member.isSuspect()) {
                suspects.add(member);
            }
        }

        if (suspects.size() == 0) {
            return null;
        }

        return suspects.get(random.nextInt(suspects.size()));
    }

    // ping suspect members once a second
    public void run() {
        synchronized (this) {
            // add ourselves to the member list
            self = new Member(registry.getIbisIdentifier(), properties);
            members.put(self.getUUID(), self);
            statistics.newPoolSize(members.size());
            registry.ibisJoined(self.getIdentifier());
        }

        long interval =
            properties.getIntProperty(RegistryProperties.PING_INTERVAL) * 1000;

        while (!registry.isStopped()) {
            cleanup();

            Member suspect = getSuspect();

            if (suspect != null) {
                if (suspect.equals(self)) {
                    logger.error("we are a suspect ourselves");
                    suspect.seen();
                } else {

                    logger.debug("suspecting " + suspect + " is dead, checking");
                    try {
                        registry.getCommHandler().ping(suspect.getIdentifier());
                        suspect.seen();
                    } catch (Exception e) {
                        logger.debug("could not reach " + suspect
                                + ", adding ourselves as witness");
                        suspect.suspectDead(registry.getIbisIdentifier());
                    }
                }
            }

            int timeout = (int) (Math.random() * interval);
            synchronized (this) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    // IGNORE
                }

            }
        }

    }
}

package ibis.ipl.impl.registry.gossip;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

class MemberSet {

    private static final Logger logger = Logger.getLogger(MemberSet.class);

    private final TypedProperties properties;

    private final Registry registry;

    private final HashSet<String> deceased;

    private final HashSet<String> left;

    private final HashMap<String, Member> members;

    MemberSet(TypedProperties properties, Registry registry) {
        this.properties = properties;
        this.registry = registry;

        deceased = new HashSet<String>();
        left = new HashSet<String>();
        members = new HashMap<String, Member>();
    }

    private synchronized Member getMember(IbisIdentifier ibis) {
        String id = ibis.getID();

        if (deceased.contains(id) || left.contains(id)) {
            return null;
        }

        Member result = members.get(id);

        if (result == null) {
            result = new Member(ibis, properties);
            members.put(id, result);
            registry.ibisJoined(ibis);
        }

        return result;
    }

    public synchronized void maybeDead(IbisIdentifier ibis) {
        Member member = getMember(ibis);

        if (member == null) {
            return;
        }

        member.addWitness(registry.getIbisIdentifier());

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

    public synchronized void writeGossipData(DataOutputStream stream) {
        // TODO Auto-generated method stub

    }

    public synchronized void readGossipData(DataInputStream stream) {
        // TODO Auto-generated method stub

    }

    /**
     * Clean up the list of members.
     */
    private synchronized void cleanup(Member member) {
        if (member.hasLeft()) {
            String id = member.getIdentifier().getID();
            left.add(member.getIdentifier().getID());
            members.remove(id);
            logger.debug("purged " + id + " from list");
        } else if (member.isDead()) {
            String id = member.getIdentifier().getID();
            deceased.add(member.getIdentifier().getID());
            members.remove(id);
        }
    }

    /**
     * Clean up the list of members.
     */
    public synchronized void cleanup() {
        // iterate over copy of values, so we can remove them if we need to
        for (Member member : members.values().toArray(new Member[0])) {
            cleanup(member);
        }
    }
}

/* $Id$ */

package ibis.gmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * The group registry keeps track of which groups there are, and deals with
 * joinGroup, findGroup, and createGroup requests. It also keeps track if
 * combined invocation structures.
 */
final class GroupRegistry implements GroupProtocol {

    private static Logger logger
            = Logger.getLogger(GroupRegistry.class.getName());

    /** Hash table for the groups. */
    private Hashtable groups;

    /** Current number of groups, used to hand out group identifications. */
    private int groupNumber;

    /** Hash table for the combined invocations. */
    private Hashtable combinedInvocations;

    /** Hash table for the combined invocations. */
    private Hashtable barriers;

    /**
     * Class for barrier information.
     */
    static private final class BarrierInfo {

        /** The barrier is full when present == size. */
        int size;

        /** The number of invocations up until now. */
        int present;

        BarrierInfo(int sz) {
            size = sz;
        }

        synchronized void addAndWaitUntilFull() {
            present++;
            if (present == size) {
                notifyAll();
            } else {
                while (present < size) {
                    try {
                        wait();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Container class for the group information that the registry maintains.
     */
    static private final class GroupRegistryData {
        /** The group interface through which this group is accessed. */
        String type;

        /** The group identification. */
        int groupNumber;

        /** The number of members in this group. */
        int groupSize;

        /** Skeleton identifications of each group member. */
        int[] memberSkels;

        /** Node identifications of each group member. */
        int[] memberRanks;

        /** Tickets for the join-replies once the group is complete. */
        int[] tickets;

        /** The number of group members that have joined this group so far. */
        int joined;

        /**
         * Constructor.
         *
         * @param groupNumber the number this group
         * @param groupSize   the number of group members
         * @param type        the group interface for this group
         */
        GroupRegistryData(int groupNumber, int groupSize, String type) {
            this.groupNumber = groupNumber;
            this.groupSize = groupSize;
            this.type = type;

            memberSkels = new int[groupSize];
            memberRanks = new int[groupSize];
            tickets = new int[groupSize];

            joined = 0;
        }
    }

    /**
     * Constructor.
     *
     * Allocates hash tables for groups and combined invocations.
     */
    public GroupRegistry() {
        combinedInvocations = new Hashtable();
        groups = new Hashtable();
        barriers = new Hashtable();
        groupNumber = 0;
    }

    /**
     * A createGroup request was received from node "rank".
     * This method creates it and writes back the result.
     *
     * @param groupName the name of the group to be created
     * @param groupSize the number of members in the group
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @param type the group interface for this new group
     * @exception java.io.IOException on an IO error.
     */
    private synchronized void newGroup(String groupName, int groupSize,
            int rank, int ticket, String type) throws IOException {

        WriteMessage w;

        w = Group.unicast[rank].newMessage();
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);

        if (groups.contains(groupName)) {
            w.writeObject(new RegistryReply(CREATE_FAILED));
        } else {
            groups.put(groupName, new GroupRegistryData(groupNumber++,
                    groupSize, type));
            w.writeObject(new RegistryReply(CREATE_OK));
        }

        w.finish();
    }

    /**
     * A joinGroup request was received from node "rank".
     * This method finds it and writes back the result.
     *
     * @param groupName the name of the group to be joined
     * @param memberSkel identification of the skeleton of the join requester
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @param interfaces the group interfaces that this requester implements
     * @exception java.io.IOException on an IO error.
     */
    private synchronized void joinGroup(String groupName, int memberSkel,
            int rank, int ticket, String[] interfaces) throws IOException {

        WriteMessage w;

        GroupRegistryData e = (GroupRegistryData) groups.get(groupName);

        if (e == null) {
            w = Group.unicast[rank].newMessage();
            w.writeByte(REGISTRY_REPLY);
            w.writeInt(ticket);
            w.writeObject(new RegistryReply(JOIN_UNKNOWN));
            w.finish();

        } else if (e.joined == e.groupSize) {

            w = Group.unicast[rank].newMessage();
            w.writeByte(REGISTRY_REPLY);
            w.writeInt(ticket);
            w.writeObject(new RegistryReply(JOIN_FULL));
            w.finish();

        } else {

            boolean found = false;

            for (int i = 0; i < interfaces.length; i++) {
                if (e.type.equals(interfaces[i])) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                w = Group.unicast[rank].newMessage();
                w.writeByte(REGISTRY_REPLY);
                w.writeInt(ticket);
                w.writeObject(new RegistryReply(JOIN_WRONG_TYPE));
                w.finish();
            }

            e.memberSkels[e.joined] = memberSkel;
            e.memberRanks[e.joined] = rank;
            e.tickets[e.joined] = ticket;
            e.joined++;

            if (e.joined == e.groupSize) {
                for (int i = 0; i < e.groupSize; i++) {

                    w = Group.unicast[e.memberRanks[i]].newMessage();
                    w.writeByte(REGISTRY_REPLY);
                    w.writeInt(e.tickets[i]);
                    w.writeObject(new RegistryReply(JOIN_OK, e.groupNumber,
                            e.memberRanks, e.memberSkels));
                    w.finish();

                    e.tickets[i] = 0;
                }
                e.tickets = null;
            }
        }
    }

    /**
     * A findGroup request was received from node "rank".
     * This method finds it and writes back the result.
     *
     * @param groupName the name of the group to be found
     * @param rank the node identification of the requester
     * @param ticket ticket number for the reply
     * @exception java.io.IOException on an IO error.
     */
    private synchronized void findGroup(String groupName, int rank, int ticket)
            throws IOException {

        WriteMessage w;

        GroupRegistryData e = (GroupRegistryData) groups.get(groupName);
        w = Group.unicast[rank].newMessage();
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);

        if (e == null) {
            w.writeObject(new RegistryReply(GROUP_UNKNOWN));
        } else if (e.joined != e.groupSize) {
            w.writeObject(new RegistryReply(GROUP_NOT_READY));
        } else {
            w.writeObject(new RegistryReply(GROUP_OK, e.type, e.groupNumber,
                    e.memberRanks, e.memberSkels));
        }
        w.finish();
    }

    /**
     * Deals with a request for a combined invocation info structure. It waits until
     * all invokers have made such a request, and then writes back the
     * requested information.
     *
     * @param r the request message
     * @exception java.io.IOException on an IO error.
     */
    private void defineCombinedInvocation(ReadMessage r) throws IOException {
        String name;
        String method;
        int rank;
        int cpu;
        int ticket;
        int size;
        int mode;
        int groupID;

        groupID = r.readInt();
        cpu = r.readInt();
        ticket = r.readInt();
        name = r.readString();
        method = r.readString();
        rank = r.readInt();
        size = r.readInt();
        mode = r.readInt();
        r.finish();

        String id = name + "?" + method + "?" + groupID;
        CombinedInvocationInfo inf;

        synchronized (this) {
            inf = (CombinedInvocationInfo) combinedInvocations.get(id);
            if (inf == null) {
                inf = new CombinedInvocationInfo(groupID, method, name, mode,
                        size);
                combinedInvocations.put(id, inf);
            }

            if (inf.mode != mode || inf.numInvokers != size
                    || inf.present == size) {
                WriteMessage w = Group.unicast[cpu].newMessage();
                w.writeByte(REGISTRY_REPLY);
                w.writeInt(ticket);
                w.writeObject(new RegistryReply(COMBINED_FAILED,
                        inf.present == size ? "Combined invocation full"
                                : "Inconsistent combined invocation"));
                w.finish();
                return;
            }
        }

        inf.addAndWaitUntilFull(rank, cpu);

        WriteMessage w = Group.unicast[cpu].newMessage();
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);
        w.writeObject(new RegistryReply(COMBINED_OK, inf));
        w.finish();
    }

    /**
     * Deals with a barrier request. It waits until
     * all invokers have made such a request, and then writes back
     * to all.
     *
     * @param r the request message
     * @exception java.io.IOException on an IO error.
     */
    private void doBarrier(ReadMessage r) throws IOException {

        int ticket = r.readInt();
        String id = r.readString();
        int size = r.readInt();
        int cpu = r.readInt();
        r.finish();

        BarrierInfo inf;

        synchronized (this) {
            inf = (BarrierInfo) barriers.get(id);
            if (inf == null) {
                inf = new BarrierInfo(size);
                barriers.put(id, inf);
            }

            if (inf.size != size) {
                WriteMessage w = Group.unicast[cpu].newMessage();
                w.writeByte(REGISTRY_REPLY);
                w.writeInt(ticket);

                w.writeObject(new RegistryReply(BARRIER_FAILED,
                        "Inconsistent barrier size"));
                w.finish();
                return;
            }
        }

        inf.addAndWaitUntilFull();

        synchronized (this) {
            inf = (BarrierInfo) barriers.get(id);
            if (inf != null) {
                barriers.remove(id);
            }
        }

        WriteMessage w = Group.unicast[cpu].newMessage();
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);
        w.writeObject(new RegistryReply(BARRIER_OK));
        w.finish();
    }

    /**
     * Reads a request from the message and deals with it.
     *
     * @param r the message
     */
    public void handleMessage(ReadMessage r) {
        try {

            byte opcode;

            int rank;
            String name;
            String type;
            String[] interfaces;
            int size;
            int ticket;
            int memberSkel;

            opcode = r.readByte();

            switch (opcode) {
            case CREATE_GROUP:
                rank = r.readInt();
                ticket = r.readInt();
                name = r.readString();
                type = r.readString();
                size = r.readInt();
                r.finish();

                logger.debug(Group._rank + ": Got a CREATE_GROUP("
                        + name + ", " + type + ", " + size + ") from "
                        + rank + " ticket(" + ticket + ")");

                newGroup(name, size, rank, ticket, type);

                logger.debug(Group._rank + ": CREATE_GROUP(" + name
                        + ", " + type + ", " + size + ") from " + rank
                        + " HANDLED");
                break;

            case JOIN_GROUP:
                rank = r.readInt();
                ticket = r.readInt();
                name = r.readString();
                interfaces = (String[]) r.readObject();
                memberSkel = r.readInt();
                r.finish();

                logger.debug(Group._rank + ": Got a JOIN_GROUP("
                        + name + ", " + interfaces + ") from " + rank);

                joinGroup(name, memberSkel, rank, ticket, interfaces);
                break;

            case FIND_GROUP:
                rank = r.readInt();
                ticket = r.readInt();
                name = r.readString();
                r.finish();

                logger.debug(Group._rank + ": Got a FIND_GROUP(" + name + ")");

                findGroup(name, rank, ticket);
                break;

            case DEFINE_COMBINED:
                defineCombinedInvocation(r);
                break;

            case BARRIER:
                doBarrier(r);
                break;
            }

        } catch (Exception e) {
            /* TODO: is this a good way to deal with an exception? */
            System.out.println(Group._rank + ": Error in GroupRegistry " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}

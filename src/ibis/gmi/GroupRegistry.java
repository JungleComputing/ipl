/* $Id$ */

package ibis.gmi;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.ReadMessage;

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
            = ibis.util.GetLogger.getLogger(GroupRegistry.class.getName());

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
        final String type;

        /** The group identification. */
        final int groupNumber;

        /** The number of members in this group. */
        final int groupSize;
        
        /** List of available ranks */
        private final boolean [] rankAvailable;
        
        /** Skeleton identifications of each group member. */
        private int[] skeletons;

        /** Node identifications of each group member. */
        private int[] machineRanks;

        /** Requested rank for each group member. */        
        private int[] requestedRanks; 
        
        /** Tickets for the join-replies once the group is complete. */
        private int[] tickets;

        /** The number of group members that have joined this group so far. */
        private int joined;
        
        /** Indicates if the group if full */
        private boolean full = false;
        
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
            
            rankAvailable = new boolean[groupSize];
            skeletons = new int[groupSize];
            machineRanks = new int[groupSize];
            requestedRanks = new int[groupSize];
            tickets = new int[groupSize];

            for (int i=0;i<groupSize;i++) { 
                rankAvailable[i] = true;                
            }
            
            joined = 0;
        }        
        
        /** Is the group full yet ? */
        boolean full() { 
            return full;            
        }
        
        /** Is the given rank legal in this group ? */
        boolean legalRank(int rank) {
            // NOTE: rank -1 implies "don't care", so it's legal    
            return (rank >= -1 && rank < groupSize);
        }
        
        /** Is the given rank still available ? */
        boolean rankAvailable(int rank) {                       
            return !full && (rank == -1 || rankAvailable[rank]);                       
        }
        
        /** Does the list of interfaces contain this group its type ? */
        boolean checkType(String [] interfaces) { 
            
            for (int i = 0; i < interfaces.length; i++) {
                if (type.equals(interfaces[i])) {
                    return true;                   
                }
            }            
            return false;
        } 
        
        /** 
         * Join the group. The request is simply added to the next slot.
         * After the group is full, all entries are reordered to satify 
         * the rank requests of all machines. 
         * 
         * Note that the various sanity checks above must be used before
         * calling this method to ensure that the group is not full, the 
         * rank is legal and unused, the group type is correct, etc.    
         */
        void join(int skeleton, int machineRank, int requestedRank, int ticket) { 
            
            if (requestedRank != -1) { 
                rankAvailable[requestedRank] = false;
            }
            
            skeletons[joined] = skeleton;
            machineRanks[joined] = machineRank;
            requestedRanks[joined] = requestedRank; 
            tickets[joined] = ticket;    
            joined++;   
            
            if (joined == groupSize) { 
                full = true;
                reorder();
            } 
        }
        
        /** 
         * Reorder the requests so that the rank request of the machines are 
         * satisfied. Do this by traversing the entries twice. The first round 
         * stores all entries which have requested a specific rank into the right
         * place, the second round fills the left over spots with the left over 
         * entries (which did not request a rank).
         */
        private void reorder() {

            // First create some datastructures
            boolean [] used = new boolean[groupSize];
            
            int [] m = new int[groupSize];
            int [] s = new int[groupSize];
            int [] t = new int[groupSize];
            
            // Now put all entries which requested a rank 
            // into the right place. 
            for (int i=0;i<groupSize;i++) {                
                if (requestedRanks[i] != -1) {
                    // must reorder this entry
                    int r = requestedRanks[i];
                    
                    m[r] = machineRanks[i];
                    s[r] = skeletons[i];
                    t[r] = tickets[i];
                    used[i] = true;
                } 
            } 
              
            // Next, fill the empty spots with the left over 
            // entries.
            int next = 0;            
            for (int i=0;i<groupSize;i++) {                
                if (requestedRanks[i] == -1) {
                    // find an empty slot for this entry
                    while (used[next]) { 
                        next++;
                    } 
                  
                    m[next] = machineRanks[i];
                    s[next] = skeletons[i];
                    t[next] = tickets[i];
                    next++;
                } 
            } 
            
            // Finally, replace the old datastructures with the 
            // new ones and clear some data which is no longer 
            // needed.
            machineRanks = m;
            skeletons = s;
            tickets = t;              
        } 
        
        /**
         * @return machineRank array (iff the group is full) or null 
         */
        int [] getRankedMachines() {
            return (full ? machineRanks : null);
        }
                
        /**
         * @return skeleton array (iff the group is full) or null
         */
        int [] getRankedSkeletons() { 
            return (full ? skeletons : null);            
        }   

        /**
         * @return ticket array (iff the group is full) or null
         */
        int [] getRankedTickets() { 
            
            // NOTE: the tickets can only be used once, so
            // they are cleared after use. 
            if (!full) {
                return null;
            }
             
            int [] temp = tickets;
            tickets = null;
            return temp;            
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
    private void newGroup(ReadMessage r) throws IOException {

        int machineRank = r.readInt();
        int ticket = r.readInt();
        String groupName = r.readString();
        String type = r.readString();
        int groupSize = r.readInt();        
        SendPort sender = Group.getUnicastSendPort(r.origin().ibis());        
        r.finish();
                        
        if (logger.isDebugEnabled()) {
            logger.debug(Group.rank() + ": GroupRegistry.newGroup("
                    + groupName + ", " + groupSize + ", " + sender + ", " + 
                    ticket + ", " + type);                    
        }
        
        boolean exists;
        
        synchronized (groups) {           
            exists = groups.contains(groupName);
            
            if (!exists) { 
                groups.put(groupName, new GroupRegistryData(groupNumber++,
                        groupSize, type));
            }
        }
                
        //WriteMessage w = Group.unicast[rank].newMessage();
        WriteMessage w = sender.newMessage();        
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);

        if (exists) { 
            w.writeObject(new RegistryReply(CREATE_FAILED));
        } else {
            w.writeObject(new RegistryReply(CREATE_OK));
        }

        w.finish();
        
        if (logger.isDebugEnabled()) { 
            logger.debug(Group.rank() + ": CREATE_GROUP(" + groupName
                    + ", " + type + ", " + groupSize + ") from " + machineRank
                    + " HANDLED");
        }
    }

    /**
     * A joinGroup request was received from node "rank".
     * This method finds it and writes back the result.
     *
     * @param groupName the name of the group to be joined
     * @param memberSkel identification of the skeleton of the join requester
     * @param machineRank the node identification of the requester
     * @param requestedRank the group rank the requester would like to have
     * @param ticket ticket number for the reply
     * @param interfaces the group interfaces that this requester implements
     * @exception java.io.IOException on an IO error.
     * @throws ClassNotFoundException 
     */
    private void joinGroup(ReadMessage r) throws IOException, 
        ClassNotFoundException { 
            
        int machineRank = r.readInt();
        int requestedRank = r.readInt();
        int ticket = r.readInt();
        String groupName = r.readString();
        String [] interfaces = (String[]) r.readObject();
        int memberSkel = r.readInt();
        long timeout = r.readLong();
        
        SendPort sender = Group.getUnicastSendPort(r.origin().ibis());
                        
        r.finish();
                       
        // TODO Implement timeout.         
        if (logger.isDebugEnabled()) {
            logger.debug(Group.rank() + ": GroupRegistry.joinGroup(" + 
                    groupName +", " + memberSkel + ", " + machineRank + ", " + 
                    requestedRank + ", " + ticket + ", " + interfaces + ", " + 
                    timeout +")");                      
        }

        GroupRegistryData e = null;
        RegistryReply reply = null;
        boolean last = false;
        
        synchronized (groups) {
            e = (GroupRegistryData) groups.get(groupName);

            if (e == null) {
                
                // Check if the group exists
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                            "- Group \"" + groupName + "\" not found!");                      
                }
                
                reply = new RegistryReply(JOIN_UNKNOWN);
                
            } else if (e.full()) {
                
                // Check if the group is full                
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                            "- Group \"" + groupName + "\" full!");                      
                }
                
                reply = new RegistryReply(JOIN_FULL);
                
            } else if (!e.legalRank(requestedRank)) {
                
                // Check if the requested rank is legal
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                            "- Group \"" + groupName + "\" - Rank " + requestedRank +
                            " not legal!");                      
                }
                
                reply = new RegistryReply(JOIN_ILLEGAL_RANK);
            
            } else if (!e.rankAvailable(requestedRank)) {

                // Check if the requested rank is still available       
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                            "- Group \"" + groupName + "\" - Rank " + requestedRank +
                            " not available!");                      
                }
                
                reply = new RegistryReply(JOIN_RANK_TAKEN);
                
            } else if (!e.checkType(interfaces)) {
                
                // Check it the type of the group is correct                 
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                            " - Group \"" + groupName + "\" - Rank " + requestedRank +
                            " not legal!");                      
                }
                
                reply = new RegistryReply(JOIN_WRONG_TYPE);
            } else { 

                // All is well, so join the group    
                e.join(memberSkel, machineRank, requestedRank, ticket);
            
                // Check if this was the last member        
                last = e.full();
            }
        }             

        if (reply != null) { 
            // The lookup failed, so send the error back 
        
            WriteMessage w = sender.newMessage();
            w.writeByte(REGISTRY_REPLY);
            w.writeInt(ticket);
            w.writeObject(reply);
            w.finish();
            return;
            
        } 
        
        if (last) {    
                
            // The lookup was succesfull, but only the last one replies....                                    
            if (logger.isDebugEnabled()) {
                logger.debug(Group.rank() + ": GroupRegistry.joinGroup " + 
                        "- Group " + groupName + " full, sending replies!");                      
            }
            
            int [] machines = e.getRankedMachines();
            int [] skeletons = e.getRankedSkeletons();
            int [] tickets = e.getRankedTickets();
                                                          
            for (int i = 0; i < e.groupSize; i++) {
                WriteMessage w = Group.unicast[machines[i]].newMessage();
                w.writeByte(REGISTRY_REPLY);
                w.writeInt(tickets[i]);
                w.writeObject(new RegistryReply(JOIN_OK, e.groupNumber, 
                        machines, skeletons));
                w.finish();
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
    private void findGroup(ReadMessage r) throws IOException {
        
        int machineRank = r.readInt();
        int ticket = r.readInt();
        String groupName = r.readString();
        
        SendPort sender = Group.getUnicastSendPort(r.origin().ibis());
        
        r.finish();

        if (logger.isDebugEnabled()) {
            logger.debug(Group.rank() + ": GroupRegistry.findGroup("
                    + groupName + ", " + machineRank + ", " + ticket + ")");                    
        }
        
        RegistryReply reply = null;
        
        synchronized (groups) {        
            GroupRegistryData e = (GroupRegistryData) groups.get(groupName);
            
            if (e == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.findGroup " + 
                            "- Group \"" + groupName + "\" not found!");                      
                }
                
                reply = new RegistryReply(GROUP_UNKNOWN);
            } else if (!e.full()) {
                
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.findGroup " + 
                            "- Group \"" + groupName + "\" not ready!");                      
                }
                
                reply = new RegistryReply(GROUP_NOT_READY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(Group.rank() + ": GroupRegistry.findGroup " + 
                            "- Group \"" + groupName + "\" ok!");                      
                }
                                
                reply = new RegistryReply(GROUP_OK, e.type, e.groupNumber,
                        e.getRankedMachines(), e.getRankedSkeletons());
            }             
        }

        WriteMessage w = sender.newMessage();
        w.writeByte(REGISTRY_REPLY);
        w.writeInt(ticket);
        w.writeObject(reply);
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
   
        int groupID = r.readInt();
        int cpu = r.readInt();
        int ticket = r.readInt();
        String name = r.readString();
        String method = r.readString();
        int rank = r.readInt();
        int size = r.readInt();
        int mode = r.readInt();
        
        SendPort sender = Group.getUnicastSendPort(r.origin().ibis());
                
        r.finish();

        String id = name + "?" + method + "?" + groupID;
        CombinedInvocationInfo inf;

        synchronized (combinedInvocations) {
            inf = (CombinedInvocationInfo) combinedInvocations.get(id);
            if (inf == null) {
                inf = new CombinedInvocationInfo(groupID, method, name, mode,
                        size);
                combinedInvocations.put(id, inf);
            }

            if (inf.mode != mode || inf.numInvokers != size
                    || inf.present == size) {
                WriteMessage w = sender.newMessage();
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

        WriteMessage w = sender.newMessage();
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
        
        SendPort sender = Group.getUnicastSendPort(r.origin().ibis());
                
        r.finish();

        BarrierInfo inf;

        synchronized (this) {
            inf = (BarrierInfo) barriers.get(id);
            if (inf == null) {
                inf = new BarrierInfo(size);
                barriers.put(id, inf);
            }

            if (inf.size != size) {
                WriteMessage w = sender.newMessage();
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

        WriteMessage w = sender.newMessage();
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
            byte opcode = r.readByte();

            switch (opcode) {
            case CREATE_GROUP:
                newGroup(r);
                break;

            case JOIN_GROUP:
                joinGroup(r);
                break;

            case FIND_GROUP:                
                findGroup(r);
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
            logger.fatal(Group.rank() + ": Error in GroupRegistry ", e);            
            System.exit(1);
        }
    }
}

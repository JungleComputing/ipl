/* $Id$ */

package ibis.gmi;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoMatchingIbisException;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.Ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

/**
 * The {@link Group} class takes care of the startup, and has methods
 * to create, join, lookup, and exit a group.
 */
public final class Group implements GroupProtocol,
        ibis.ipl.PredefinedCapabilities {

    static {
        Logger ibisLogger = Logger.getLogger("ibis");
        Logger rootLogger = Logger.getRootLogger();
        if (! rootLogger.getAllAppenders().hasMoreElements()
             && !ibisLogger.getAllAppenders().hasMoreElements()) {
            // No appenders defined, print to standard err by default
            PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p %m%n");
            WriterAppender appender = new WriterAppender(layout, System.err);
            ibisLogger.addAppender(appender);
            ibisLogger.setLevel(Level.WARN);
        }
    }

    static Logger logger = Logger.getLogger(Group.class.getName());
     
    /** Ibis rank number in this run. */
    static int _rank;

    /** The total number of nodes involved in this run. */
    private static int _size;
    
    /** To get tickets from. */
    static Ticket ticketMaster = null;
    
    /** The group registry, only lives on the node elected as the master. */
    static GroupRegistry registry;

    /**
     * Local cache of stub classes, so that not every group lookup has to
     * go through the registry.
     */
    private static Hashtable<String, GroupStubData> stubclasses;

    /** My local ibis. */
    static Ibis ibis;

    /** Ibis registry, used for setting up stuff. */
    private static Registry ibisRegistry;

    /** Port types for ports used in GMI. */
    private static CapabilitySet portTypeSystemUcast;    
    private static CapabilitySet portTypeSystemMcast;    
    private static CapabilitySet portTypeManyToOne;
   // private static CapabilitySet portTypeOneToMany;
      
    /** Unicast send ports, one for each destination node. */
    static SendPort[] unicast;
    
    /** Port on which unicast messages are received. */
    private static ReceivePort receivePort;
    
    /** Currently allocated multicast send ports. */
//    private static Hashtable<String, SendPort>  multicastSendports;
    
    /** For receiving messages from the GMI master. */
    private static ReceivePort systemIn;

    /** For sending messages to the GMI master. */
    private static SendPort systemOut;

    /** ReceivePort identifiers, one for each node. */
    private static ReceivePortIdentifier[] pool;

    /** Upcall handler. */
  //  private static GroupCallHandler groupCallHandler;

    /** Skeletons available through group identification. */
    private static ArrayList<GroupSkeleton> groups;

    /** Skeletons on this node. */
    private static ArrayList<GroupSkeleton> skeletons;

    /** Stubs and stub identifications. */
    private static ArrayList<GroupStub> stubIDStack;

    /** The stub counter, used to allocate stubs. */
    static int stubCounter;

    /** Name of this host (actually, its Ibis name). */
    static String name;

    /**
     * Container class for group information.
     */
    private static final class GroupStubData {

        /** The group name. */
        String groupName;

        /** The name of the group interface for this group. */
        String typeName;


        /**
         * The stub class for this group, so that stubs can easily be
         * created.
         */
        Class stubClass;

        /** The group identification. */
        int groupID;

        /** The node identifications for all group members. */
        int[] memberRanks;

        /** The skeleton identifications for all group members. */
        int[] memberSkels;

       
    }

    /**
     * Initialization code. Starts up Ibis, elects a node to run the
     * group registry, creates send and receive ports.
     */
    static {
        try {
            ticketMaster = new Ticket();
            groups = new ArrayList<GroupSkeleton>();            
            skeletons = new ArrayList<GroupSkeleton>();
            stubIDStack = new ArrayList<GroupStub>();
            
            stubclasses = new Hashtable<String, GroupStubData>();
 //           multicastSendports = new Hashtable<String, SendPort> ();

            if (logger.isDebugEnabled()) {
                logger.debug("?: <static> - Init Group RTS");
            }

            CapabilitySet reqprops = new CapabilitySet(
                SERIALIZATION_OBJECT, WORLDMODEL_CLOSED, CONNECTION_MANY_TO_ONE,
                CONNECTION_ONE_TO_MANY, COMMUNICATION_RELIABLE,
                RECEIVE_AUTO_UPCALLS, RECEIVE_EXPLICIT);
            try {
                ibis = IbisFactory.createIbis(reqprops, null, null, null);
            } catch (NoMatchingIbisException e) {
                logger.warn("?: <static> - " + 
                        "Could not find an Ibis that can run this "
                        + "GMI implementation");
                System.exit(1);
            }
            
            name = ibis.identifier().toString();
            ibisRegistry = ibis.registry();

            // Create the four port types used in GMI  

            // System unicast (many to one) port type 
            CapabilitySet portTypeSystemUcast = new CapabilitySet(
                    SERIALIZATION_OBJECT, CONNECTION_MANY_TO_ONE,
                    COMMUNICATION_RELIABLE, RECEIVE_AUTO_UPCALLS,
                    RECEIVE_EXPLICIT);
            
            // System unicast (many to one) port type 
            portTypeSystemMcast = new CapabilitySet(
                    SERIALIZATION_OBJECT, CONNECTION_ONE_TO_MANY,
                    COMMUNICATION_RELIABLE, RECEIVE_EXPLICIT);
           
            // Unicast (many to one) port type            
            portTypeManyToOne = new CapabilitySet(
                    SERIALIZATION_OBJECT, CONNECTION_MANY_TO_ONE,
                    COMMUNICATION_RELIABLE, RECEIVE_AUTO_UPCALLS);

            // Multicast (one to many) port type            
            CapabilitySet props = new CapabilitySet(
                    SERIALIZATION_OBJECT, CONNECTION_ONE_TO_MANY,
                    COMMUNICATION_RELIABLE, RECEIVE_AUTO_UPCALLS);

            MulticastGroups.init(props);

            // Create the unicast receive port
            receivePort = ibis.createReceivePort(portTypeManyToOne, "GMI port",
                    new GroupCallHandler());            
            receivePort.enableConnections();
            
            _size = ibis.totalNrOfIbisesInPool();
                            
            IbisIdentifier master = ibisRegistry.elect("GMI MASTER ELECTION");
            
            if (master.equals(ibis.identifier())) { 
                // I am the master
            
                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": <static> - " + name +
                            " I am master");
                }

                registry = new GroupRegistry();
                
                _rank = 0;

                pool = new ReceivePortIdentifier[_size];
                pool[0] = receivePort.identifier();

                if (_size > 1) {

                    systemIn = ibis.createReceivePort(portTypeSystemUcast,
                            "Master");
                    systemIn.enableConnections();

                    systemOut = ibis.createSendPort(portTypeSystemMcast, "Master");

                    for (int j = 1; j < _size; j++) {
                        ReadMessage r = systemIn.receive();
                        ReceivePortIdentifier reply
                                = (ReceivePortIdentifier) r.readObject();
                        ReceivePortIdentifier id
                                = (ReceivePortIdentifier) r.readObject();
                        r.finish();

                        systemOut.connect(reply);
                        pool[j] = id;
                    }

                    WriteMessage w = systemOut.newMessage();
                    w.writeObject(pool);
                    w.finish();
                }
            } else {
                systemIn = ibis.createReceivePort(portTypeSystemMcast, "Client");
                systemIn.enableConnections();

                systemOut = ibis.createSendPort(portTypeSystemUcast, "Client");

                systemOut.connect(master, "Master");

                WriteMessage w = systemOut.newMessage();
                w.writeObject(systemIn.identifier());
                w.writeObject(receivePort.identifier());
                w.finish();

                ReadMessage r = systemIn.receive();
                pool = (ReceivePortIdentifier []) r.readObject();
                r.finish();
                
                for (int j = 1; j < _size; j++) {
                    if (pool[j].equals(receivePort.identifier())) {
                        _rank = j;
                        break;
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": <static> - " + 
                            name + " I am client");
                }

            }

            unicast = new SendPort[_size];

            for (int j = 0; j < _size; j++) {
                unicast[j] = ibis.createSendPort(portTypeManyToOne,
                        "Unicast on " + name + " to " + j);

                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": <static> - " + 
                            "Connecting unicast sendport "
                            + unicast[j].name());
                }

                unicast[j].connect(pool[j]);

                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": <static> - " + 
                            "Connecting unicast sendport "
                            + unicast[j].name() + " done");
                }
            }

            if (_size > 1) {
                if (_rank == 0) {
                    for (int j = 1; j < _size; j++) {
                        ReadMessage r = systemIn.receive();
                        r.finish();
                    }

                    WriteMessage w = systemOut.newMessage();
                    w.finish();
                } else {
                    WriteMessage w = systemOut.newMessage();
                    w.finish();
                    ReadMessage r = systemIn.receive();
                    r.finish();
                }
            }

            receivePort.enableUpcalls();

            if (logger.isDebugEnabled()) {
                logger.debug(_rank + ": <static> - " + name + ": Group init");
            }

            /****
             * This is only supported in SDK 1.3 and upwards. Comment out
             * if you run an older SDK.
             */
            Runtime.getRuntime().addShutdownHook(
                    new Thread("gmi.Group ShutdownHook") {
                        public void run() {
                            if (ibis != null) {
                                try {
                                    ibis.end();
                                } catch (IOException e) {
                                    logger.error(name + ": in ShutdownHook: ", e);
                                }
                                ibis = null;
                            }
                            // System.err.println("Ended Ibis");
                        }
                    });
            /* End of 1.3-specific code */

        } catch (Exception e) {
            logger.fatal(name + ": Could not init Group RTS " + e, e);
            System.exit(1);
        }
    }

    /** Disable construction. */
    protected Group() {
    	// nothing here
    }


    /**
     * Gets a multicast send port using an identification to see if we
     * already have such a send port. If not, a new one is created.
     *
     * @param ID    identification of the send port
     * @param hosts the target hosts of the multicast
     * @return the multicast send port.
     */
/*    public static SendPort getMulticastSendport(String ID, int[] hosts) {

        // Note: for efficiency the ranks in hosts should be sorted
        // (low->high) !!!

        if (hosts.length == 1) {
            return unicast[hosts[0]];
        }
     
        if (logger.isDebugEnabled()) { 
            logger.debug(_rank + ": Group.getMulticastSendport - " + 
                    "Looking for multicast sendport " + ID);
        }

        SendPort temp = multicastSendports.get(ID);

        System.err.println("Should now create receiveports " + _rank + "-" + ID);
        
        if (temp == null) {
            // there is no multicast sendport to this combination of hosts yet.
            // so create it and add it to the table.
            if (logger.isDebugEnabled()) { 
                logger.debug(_rank + ": Group.getMulticastSendport - " + 
                        "Creating multicast sendport " + ID);
            }
          
            try {
                temp = portType.createSendPort("Multicast on " + name + " to "
                        + ID);
            } catch (IOException e) {
                logger.fatal(name + ": Could not create multicast group "
                        + ID + " " + e, e);
                System.exit(1);
            }

            try {
                for (int i = 0; i < hosts.length; i++) {
                    temp.connect(pool[hosts[i]]);
                    
                    if (logger.isDebugEnabled()) { 
                        logger.debug(_rank + ": Group.getMulticastSendport - " +    
                                "Connected to " + hosts[i] + ", " + pool[hosts[i]].name());
                    }
                }
            } catch (IOException e) {
                logger.fatal(name
                        + ": Could not interconnect multicast group " + ID
                        + " " + e, e);
                System.exit(1);
            }

            multicastSendports.put(ID, temp);
        }
        
        if (logger.isDebugEnabled()) { 
            logger.debug(_rank + ": Group.getMulticastSendport - " +                     
                    "Found multicast send port " + temp);
        }
        return temp;
    }
*/
    
    
    /**
     * Gets a new (local) skeleton identification.
     *
     * @param skel the group skeleton
     * @return the new skeleton identification.
     */
    protected static synchronized int getNewSkeletonID(GroupSkeleton skel) {

        skeletons.add(skel);
        Group.class.notifyAll();
        return skeletons.size()-1;
    }

    /**
     * Gets a skeleton through its local identification.
     * @param skel the local skeleton identification
     * @return the group skeleton.
     */
    static synchronized GroupSkeleton getSkeleton(int skel) {

        GroupSkeleton tmp = skeletons.get(skel);

        while (tmp == null) {
            try { 
                Group.class.wait();
            } catch (InterruptedException e) { 
                // ignore
            }   
        }

        return tmp;
    }
    
    /**
     * Makes a skeleton available through its group identification.
     *
     * @param groupID  the group identification
     * @param skeleton the group skeleton to be made available
     */
    protected static synchronized void registerGroupMember(int groupID,
            GroupSkeleton skeleton) {

        if (logger.isDebugEnabled()) {
            logger.debug(_rank + ": <static> - " + 
                    "Group.registerGroupMember(" + groupID + " "
                    + skeleton.getClass().getName());
        }
        
        // For some reason, ArrayList does not allow 'empty' slots, so  
        // explicitly fill them with <null> values.  
        while (groups.size() <= groupID) { 
            groups.add(null);
        }
        
        groups.add(groupID, skeleton);        
        Group.class.notifyAll();
    }
   
    /**
     * Gets a skeleton through its group identification.
     * @param groupID the group identification
     * @return the group skeleton
     */
    static synchronized GroupSkeleton getSkeletonByGroupID(int groupID) {

        while (groupID >= groups.size() || groups.get(groupID) == null) {
            try {
                Group.class.wait();
            } catch (InterruptedException e) {
                // ignore
            } 
        }  

        return groups.get(groupID);
    }

    /**
     * Creates a group.
     *
     * @param nm  the name of the group to be created
     * @param type  the group interface that this group will provide
     * @param size  the number of group members that this group will have
     * @exception RuntimeException when the group already exists or in case
     * of a communication error.
     */
    public static void create(String nm, Class type, int size)
            throws RuntimeException {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(_rank + ": create(" + nm + ", " + type
                        + ", " + size + ") starting");
            }
            
            // Check if the size is legal
            if (size <= 0) { 
                throw new RuntimeException("Illegal group size (" + size 
                        + ") specified!");
            }

            // Check if the name is legal 
            if (nm == null || nm.length() == 0) { 
                throw new RuntimeException("Illegal group name (" + name
                        + ") specified!");
            }
                      
            // Check if the type is legal 
            if (type == null || !type.isInterface() ||
                    !GroupInterface.class.isAssignableFrom(type)) { 
                throw new RuntimeException("Illegal group type (" + type
                        + ") specified!");                
            }
            
            int ticket = ticketMaster.get();

            WriteMessage w = unicast[0].newMessage();
            w.writeByte(GroupProtocol.REGISTRY);
            w.writeByte(CREATE_GROUP);
            w.writeInt(_rank);
            w.writeInt(ticket);
            w.writeString(nm);
            w.writeString(type.getName());
            w.writeInt(size);
            w.finish();

            logger.debug(_rank + ": Group.create(" + nm + ", " + size
                    + ") waiting for reply on ticket(" + ticket + ")");

            RegistryReply r = (RegistryReply) ticketMaster.collect(ticket);

            if (r.result == CREATE_FAILED) {
                throw new RuntimeException(_rank + " Group.create(" + nm + ", "
                        + size + ") Failed : Group already exists!");
            }

            logger.debug(_rank + ": Group.create(" + nm + ", " + size
                    + ") done");

        } catch (IOException e) {
            throw new RuntimeException(_rank + " Group.create(" + nm + ", "
                    + size + ") Failed : communication error !"
                    + e.getMessage());
        }
    }

    /**
     * Joins the group with this name, by communicating with
     * the group registry, and blocks until the group is ready.
     *
     * @param nm  the name of the group we are joining
     * @param o   the member that is joining the group
     * @param rank the rank that the member wishes to have in the group
     * @param timeout the maximum time the operation may block (in milliseconds). 
     * Join will block indefinitely if timeout is 0 or smaller.  
     * @exception RuntimeException when the group is not found,
     *            the group is already full, or something else goes wrong.
     */
    public static void join(String nm, GroupMember o, int rank, long timeout) throws RuntimeException {
        
        int groupnumber = 0;
        int[] memberRanks = null;
        int[] memberSkels = null;
        boolean retry;
        int ticket;
        WriteMessage w;
        RegistryReply r;
        RuntimeException exception = null;
        
        if (logger.isDebugEnabled()) {
            logger.debug(_rank + ": join(" + nm + ", " + o
                    + ", " + rank + ") starting");
        }

        try {
            do { 
                retry = false;

                ticket = ticketMaster.get();

                w = unicast[0].newMessage();
                w.writeByte(GroupProtocol.REGISTRY);
                w.writeByte(JOIN_GROUP);
                w.writeInt(_rank);
                w.writeInt(rank);                
                w.writeInt(ticket);
                w.writeString(nm);
                w.writeObject(o.groupInterfaces);
                w.writeInt(o.mySkel);
                w.writeLong(timeout);
                w.finish();

                logger.debug(_rank + ": join(" + nm
                        + ") waiting for reply on ticket(" + ticket + ")");

                r = (RegistryReply) ticketMaster.collect(ticket);

                switch (r.result) {
                case JOIN_UNKNOWN:

                    if (logger.isDebugEnabled()) {
                        logger.debug(_rank + ": join(" + nm
                                + ", " + rank + ") group not found, retry");
                    }
                    retry = true;
                    break;

                case JOIN_WRONG_TYPE:
                    exception = new RuntimeException(_rank + " Group.join(" + nm
                            + ", " + rank + ") Failed : Group member has wrong type!");
                    break;
                    
                case JOIN_FULL:
                    exception = new RuntimeException(_rank + " Group.join(" + nm
                            + ", " + rank + ") Failed : Group full!");
                    break;
                    
                case JOIN_RANK_TAKEN:
                    exception = new RuntimeException(_rank + " Group.join(" + nm
                            + ", " + rank + ") Failed : Group rank already taken!");
                    break;
                    
                case JOIN_ILLEGAL_RANK:
                    exception = new RuntimeException(_rank + " Group.join(" + nm
                            + ", " + rank + ") Failed : Illegal rank specified!");
                    break;
                
                case JOIN_TIMEOUT:
                    exception = new RuntimeException(_rank + " Group.join(" + nm
                            + ", " + rank + ") Failed : Timeout");
                    break;
                
                case JOIN_OK:
                    groupnumber = r.groupnum;
                    memberRanks = r.memberRanks;
                    memberSkels = r.memberSkels;
                    break;
                    
                default:
                    logger.fatal(_rank + "Internal error: join(" + 
                            nm + ") Failed - Got illegal opcode");
                    System.exit(1);
                }
                
            } while (retry);
        
        } catch (Throwable e) {
            logger.warn(_rank + "Unexpected exception: join(" + 
                            nm + ") Failed - Communication error ?", e);
            
            exception = new RuntimeException(_rank + " Group.join(" + nm
                    + ") Failed - Communication error !", e);
        }    
            
        if (exception != null) {
            throw exception;        
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(_rank + ": join(" + nm + ") group("
                    + groupnumber + ") found !");
        }

        o.init(groupnumber, memberRanks, memberSkels);
            
        if (logger.isDebugEnabled()) {
            logger.debug(_rank + ": join(" + nm + ", " + o
                    + ") done");
        }
    } 
    
    /**
     * Joins the group with this name, by communicating with
     * the group registry, and blocks until the group is ready.
     *
     * @param nm  the name of the group we are joining
     * @param o     the member that is joining the group
     * @exception RuntimeException when the group is not found,
     *            the group is already full, or something else goes wrong.
     */
    public static void join(String nm, GroupMember o) throws RuntimeException {
        join(nm, o, -1, 0L);
    }

    /**
     * Joins the group with this name, by communicating with
     * the group registry, and blocks until the group is ready.
     *
     * @param nm the name of the group we are joining
     * @param o the member that is joining the group
     * @param rank the rank that the member wishes to have in the group 
     * @exception RuntimeException when the group is not found,
     *            the group is already full, or something else goes wrong.
     */
    public static void join(String nm, GroupMember o, int rank) throws RuntimeException {
        join(nm, o, rank, 0L);
    }

    /**
     * Looks up the group with this name, if necessary by communicating with
     * the group registry, and possibly waiting until the group is ready.
     *
     * @param nm  the name of the group we are looking for
     * @return the group interface we were looking for
     * @exception RuntimeException some network error has occurred
     */
    public static GroupInterface lookup(String nm) throws RuntimeException {
        return lookup(nm, -1);
    }
        
    /**
     * Looks up the group with this name, if necessary by communicating with
     * the group registry, and possibly waiting until the group is ready.
     *
     * @param nm  the name of the group we are looking for
     * @param timeout maximum time the operation may block, in milliseconds. 
     * (0 indicates that the lookup returns immediately, negative value indicates
     *  no timeout)   
     * @return the group interface we were looking for
     * @exception RuntimeException when the group is not found, or something
     *            else goes wrong.
     */
    public static GroupInterface lookup(String nm, long timeout) throws RuntimeException {
        
        GroupStubData data = stubclasses.get(nm);

        long time = System.currentTimeMillis();
               
        if (data == null) {
            // No stub info available yet, so get it from the registry.            
            data = new GroupStubData();
            
            try {                
                boolean done = false;

                while (!done) {
                    // Send registry a request.
                    int ticket = ticketMaster.get();
                    WriteMessage w = unicast[0].newMessage();
                    w.writeByte(GroupProtocol.REGISTRY);
                    w.writeByte(FIND_GROUP);
                    w.writeInt(_rank);
                    w.writeInt(ticket);
                    w.writeString(nm);
                    w.finish();

                    // Wait for the reply 
                    RegistryReply r = (RegistryReply) ticketMaster.collect(ticket);
                    
                    // Look at the reply
                    switch (r.result) {
                    
                    // Unknown group 
                    case GROUP_UNKNOWN:
                        
                        if (logger.isDebugEnabled()) {  
                            logger.debug(_rank + ": lookup(" + nm
                                    + ", " + timeout + ") - Group not known.");
                        } 
                        
                        if (timeout == 0) {                         
                            throw new RuntimeException(Group._rank
                                    + " Group.lookup(" + nm + ", " + timeout
                                    + ") Failed : unknown group!");                        
                        } 
                        break;

                    // Known, group which isn't complete yet
                    case GROUP_NOT_READY:
                        
                        if (logger.isDebugEnabled()) {  
                            logger.debug(_rank + ": lookup(" + nm
                                    + ", " + timeout + ") - Group not ready.");
                        } 
                        
                        if (timeout == 0) {
                            throw new RuntimeException(Group._rank
                                    + " Group.lookup(" + nm + ", " + timeout
                                    + ") Failed : group not ready!");
                        }                            
                        break;

                    // Found a group      
                    case GROUP_OK:
                        
                        if (logger.isDebugEnabled()) {  
                            logger.debug(_rank + ": lookup(" + nm
                                    + ", " + timeout + ") - Group OK.");
                        } 
                        
                        done = true;

                        data.groupName = nm;
                        data.typeName = r.str;
                        data.groupID = r.groupnum;
                        data.memberRanks = r.memberRanks;
                        data.memberSkels = r.memberSkels;
                        
                        String classname = "";
                        
                        try {
                            String temp = data.typeName;
                            StringTokenizer s = new StringTokenizer(temp, ".");
                            int tokens = s.countTokens();

                            if (tokens > 1) {
                                classname = s.nextToken();

                                for (int i = 1; i < tokens - 1; i++) {
                                    classname += "." + s.nextToken();
                                }
                                classname += ".";
                            }

                            classname += "group_stub_" + s.nextToken();
                            data.stubClass = Class.forName(classname);
                        } catch (Exception e) {
                            
                            if (logger.isDebugEnabled()) {  
                                logger.debug(_rank + ": lookup(" + nm
                                        + ", " + timeout + ") - class "
                                        + classname + " not found!" + e);
                            }                             
                            throw new Error(_rank + " Group.lookup(" + nm + ", "
                                    + timeout + ") - class " + classname + 
                                    " not found!", e);
                        }
                        break;

                    // Registry returned gibberish     
                    default:
                        
                        if (logger.isDebugEnabled()) {  
                            logger.debug(_rank + ": lookup(" + nm
                                    + ", " + timeout + ") - internal error - "
                                    + "got unexpected answer " + r.result);
                        }                             
                        
                        throw new Error(_rank + " Group.lookup(" + nm + ", "
                                + timeout + ") - internal error - "   
                                + "got unexpected answer " + r.result);                                           
                    }
                
                    if (!done) {  
                        // We're not done yet, but maybe there was a timeout 
                        // which has expired.
                        if (timeout > 0) { 
                            long current = System.currentTimeMillis();
                        
                            if (current - time >= timeout) {
                            
                                if (logger.isDebugEnabled()) {  
                                    logger.debug(_rank + ": lookup(" 
                                            + nm + ", " + timeout 
                                            + ") - timeout!");
                                }                             
                                                       
                                throw new RuntimeException(Group._rank
                                        + " Group.lookup(" + nm + ", " + timeout  
                                        + ") - timeout!");                              
                            }                        
                        }
                    
                        // Not sleep for a short time to prevent 
                        // overloading the GroupRegistry
                        try { 
                            Thread.sleep(250); 
                        } catch (Exception e) { 
                            // ignore
                        }
                    }
                }
                
            } catch (Exception e) {
                throw new RuntimeException(Group._rank + " Group.lookup(" + nm
                        + ") Failed : " + e);
            }

            // Add the info to the cache.
            stubclasses.put(nm, data);
        }

        return createGroupStub(data);
    }
  
    /**
     * Creates a new stub for the group.
     *
     * @return the new stub.
     */    
    protected static GroupStub createGroupStub(GroupStubData data) { 
        try {
            GroupStub s = (GroupStub) data.stubClass.newInstance();
            int stubID;
            synchronized (stubIDStack) {
                stubID = stubIDStack.size();
                stubIDStack.add(s);
            }
            s.init(data.groupID, data.memberRanks, data.memberSkels, stubID);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(Group._rank
                    + " Group.createGroupStubData: Failed to create stub of "
                    + "type " + data.typeName + " for group " + data.groupName 
                    + " " + e);
        }
    }
    
    /**
     * Finds an existing GroupStub given it's ID.
     *
     * @return the stub.
     */    
    protected static GroupStub getGroupStub(int stubID) {
        synchronized (stubIDStack) {
            return stubIDStack.get(stubID);
        }
    }
       
    /**
     * Creates a combined invocation info structure by communication with
     * the registry. Note that this method blocks until all invokers involved
     * in this combined invocation have registered (configured, in users terms).
     *
     * @param ci      the combined invocation scheme
     * @param groupID the group identification
     * @param method  the method descriptor
     * @param nm      the name used for registering this combined invocation
     * @param mode    summary of the combined invocation scheme
     * @param rank    node identification of caller
     * @param size    total number of nodes that will be involved in this
     *                combined invocation
     * @return the combined invocation info structure.
     * @exception RuntimeException when there is a failure.
     */
    protected static CombinedInvocationInfo defineCombinedInvocation(
            CombinedInvocation ci, int groupID, String method, String nm,
            int mode, int rank, int size) throws RuntimeException {
        try {
            int ticket = ticketMaster.get();
            WriteMessage w = unicast[0].newMessage();
            w.writeByte(GroupProtocol.REGISTRY);
            w.writeByte(DEFINE_COMBINED);
            w.writeInt(groupID);
            w.writeInt(_rank);
            w.writeInt(ticket);
            w.writeString(nm);
            w.writeString(method);
            w.writeInt(rank);
            w.writeInt(size);
            w.writeInt(mode);
            w.finish();

            RegistryReply r = (RegistryReply) ticketMaster.collect(ticket);

            switch (r.result) {
            case COMBINED_FAILED:
                throw new RuntimeException(r.str);
            case COMBINED_OK:
                return r.inf;
            default:
                throw new RuntimeException(
                        "Unexpected answer on DEFINE_COMBINED");
            }
        } catch (Exception e) {
            throw new RuntimeException(Group._rank
                    + " defineCombinedInvocation(" + method
                    + ") Failed : " + e);
        }
    }

    /**
     * Implements an identifier barrier with a specified size.
     *
     * @param id      the barrier identification.
     * @param cpu     the invoker
     * @param size    the number of invokers before the barrier is released.
     */
    protected static void barrier(String id, int cpu, int size) {
        try {
            int ticket = ticketMaster.get();
            WriteMessage w = unicast[0].newMessage();
            w.writeByte(GroupProtocol.REGISTRY);
            w.writeByte(BARRIER);
            w.writeInt(ticket);
            w.writeString(id);
            w.writeInt(size);
            w.writeInt(cpu);
            w.finish();

            RegistryReply r = (RegistryReply) ticketMaster.collect(ticket);

            switch (r.result) {
            case BARRIER_FAILED:
                throw new RuntimeException(r.str);
            case BARRIER_OK:
                return;
            default:
                throw new RuntimeException(
                        "Unexpected answer on DEFINE_COMBINED");
            }
        } catch (Exception e) {
            throw new RuntimeException(Group._rank + " barrier(" + id
                    + ") Failed : " + e);
        }
    }

    /**
     * Returns the node identification of the invoker.
     *
     * @return node identification.
     */
    public static int rank() {
        return _rank;
    }

    /**
     * Returns the total number of nodes in this run.
     *
     * @return total number of nodes.
     */
    public static int size() {
        return _size;
    }

    /**
     * Returns the sendport for the specified group member.
     * @param mem the group member.
     * @return the sendport for the specified group member.
     */
    public static SendPort unicast(int mem) {
        return unicast[mem];
    }

    /**
     * Exits from a group and cleans up resources. This method
     * must be called by all members of the group, making it a sort
     * of combined invocation.
     */
    public static void exit() {
        // TODO: Remove the information that is registered in the registry???
        try {

            if (_rank == 0) {

                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": exit() - " +name + " master doing exit");
                }

                if (_size > 1) {

                    for (int j = 1; j < _size; j++) {
                        ReadMessage r = systemIn.receive();
                        r.finish();
                    }

                    WriteMessage w = systemOut.newMessage();
                    w.finish();

                    systemOut.close();
                    systemIn.close();
                }

            } else {

                if (logger.isDebugEnabled()) {
                    logger.debug(_rank + ": exit() - " +name + " client doing exit");
                }

                WriteMessage w = systemOut.newMessage();
                w.finish();

                ReadMessage r = systemIn.receive();
                r.finish();

                systemOut.close();
                systemIn.close();

            }

            for (int i = 0; i < _size; i++) {
                unicast[i].close();
            }

            MulticastGroups.exit();
            
            receivePort.close();
            ibis.end();
            ibis = null;

            if (logger.isDebugEnabled()) {
                logger.debug(_rank + ": exit()- " +"Group exit done");
            } 

        } catch (Exception e) {
            logger.warn("EEEEEK", e);
        }
    }

    /**
     * Looks up the method described by descr in the group interface i.
     * If found, its {@link GroupMethod} is returned.
     *
     * @param i    the group interface in which to look
     * @param desc the method descriptor, as a string. Example format:
     *             "int lookmeup(int[],int)"
     * @return The {@link GroupMethod} object of the method.
     * @exception NoSuchMethodException is thrown when the method is not found.
     */
    public static GroupMethod findMethod(GroupInterface i, String desc)
            throws NoSuchMethodException {
        GroupStub stub = (GroupStub) i;
        GroupMethod m = stub.getMethod(desc);
        if (m == null) {
            throw new NoSuchMethodException("Method " + desc + " not found");
        }
        return m;
    }
 
    
}


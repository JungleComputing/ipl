/* $Id$ */

package ibis.gmi;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * To be part of a group, an object must implement the {@link GroupInterface}
 * and extent the {@link GroupMember} class.
 */
public class GroupMember {
    /** An identification of the group of which this is a member. */
    public int groupID;

    protected static Logger logger
            = Logger.getLogger(GroupMember.class.getName());

    /** Skeleton identifications of all group members. */
    public int[] memberSkels;

    /** Node identifications of all group members. */
    public int[] memberRanks;

    /**
     * Like memberRanks, but sorted, to create a unique string which is used
     * as identification for a multicast send port.
     */
    protected int[] multicastHosts;

    /** The string identifying the multicast send port. */
    protected String multicastHostsID;

    /** My skeleton. */
    private GroupSkeleton skeleton;

    /** Identification of my skeleton. */
    protected int mySkel;

    /** All group interfaces implemented by this member. */
    protected String[] groupInterfaces;

    /** rank within the group of this member. */
    public int myGroupRank;

    /** Size of the group of this member. */
    public int groupSize;

    /**
     * Constructor. Creates a skeleton, and figures out which group interfaces
     * are implemented.
     */
    public GroupMember() {

        if (Group.DEBUG) {
            logger.debug(Group._rank + ": constructor() starting");
        }

        try {
            String my_package = "";

            Class myClass = this.getClass();

            String temp = myClass.getName();
            StringTokenizer s = new StringTokenizer(temp, ".");

            int tokens = s.countTokens();

            /* Figure out my package name and class name. */
            for (int i = 0; i < tokens - 1; i++) {
                my_package += s.nextToken() + ".";
            }

            String my_name = s.nextToken();

            if (Group.DEBUG) {
                logger.debug(Group._rank +": constructor() my type is " + my_package
                        + my_name);
            }

            /* Now create a skeleton. */
            skeleton = (GroupSkeleton) Class.forName(
                    my_package + "group_skeleton_" + my_name).newInstance();
            mySkel = Group.getNewSkeletonID(skeleton);

            if (Group.DEBUG) {
                logger.debug(Group._rank +": constructor() skelID is " + mySkel);
            }

            Vector group_interfaces = new Vector();

            Class tempClass = myClass;

            while (tempClass != null) {
                Class[] interfaces = tempClass.getInterfaces();

                for (int i = 0; i < interfaces.length; i++) {
                    if (isGroupInterface(interfaces[i])
                            && !group_interfaces.contains(interfaces[i])) {
                        group_interfaces.add(interfaces[i]);
                    }
                }

                tempClass = tempClass.getSuperclass();
            }

            groupInterfaces = new String[group_interfaces.size()];

            if (Group.DEBUG) {
                logger.debug(Group._rank +": constructor() type "
                        + myClass.getName() + " implements the group interfaces : ");
            }

            for (int i = 0; i < group_interfaces.size(); i++) {
                groupInterfaces[i]
                        = ((Class) group_interfaces.get(i)).getName();
                logger.debug("\t" + groupInterfaces[i]);
            }

        } catch (Exception e) {
            logger.fatal(Group._rank +": constructor() could not init ", e);
            System.exit(1);
        }

        if (Group.DEBUG) {
            logger.debug(Group._rank +": GroupMember.constructor() done");
        }
    }

    /**
     * Figures out whether an interfaces is or implements the group interface.
     *
     * @param inter the class to be examined
     * @return true if it is or implements the group interface.
     */
    private boolean isGroupInterface(Class inter) {

        if (inter == ibis.gmi.GroupInterface.class) {
            return true;
        }

        Class[] parents = inter.getInterfaces();

        for (int i = 0; i < parents.length; i++) {
            if (isGroupInterface(parents[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the rank within the group of this member.
     *
     * @return the rank.
     */
    public int getRank() {
        return myGroupRank;
    }

    /**
     * Returns the size of the group of this member.
     *
     * @return the size.
     */
    public int getSize() {
        return groupSize;
    }

    /**
     * Initializes the group member when the group is complete.
     *
     * @param groupNumber the group identification
     * @param ranks the node identifications of all members in the group
     * @param skels the skeleton identifications of all members in the group
     */
    protected void init(int groupNumber, int[] ranks, int[] skels) {
        if (Group.DEBUG) {
            logger.debug(Group._rank + ": GroupMember.init() - starting");
        }

        groupID = groupNumber;
        memberRanks = ranks;
        groupSize = ranks.length;
        memberSkels = skels;

        multicastHosts = new int[groupSize];

        for (int i = 0; i < groupSize; i++) {
            if (ranks[i] == Group._rank) {
                myGroupRank = i;
            }
            multicastHosts[i] = memberRanks[i];

            if (Group.DEBUG) {
                logger.debug(Group._rank + ": GroupMember.init() - " + 
                        "Member " + i + " is on machine " + memberRanks[i]);
            }
        }
        
        /* sort multicastranks low...high */        
        Arrays.sort(multicastHosts);
                
        /* create a multicast ID */
        StringBuffer buf = new StringBuffer("");

        for (int i = 0; i < multicastHosts.length; i++) {
            buf.append(multicastHosts[i]);
            buf.append(".");
        }

        multicastHostsID = buf.toString();

        /* init the skeleton */
        skeleton.init(this);
        Group.registerGroupMember(groupID, skeleton);

        if (Group.DEBUG) {
            logger.debug(Group._rank + ": GroupMember.init() - " + 
                    "myGroupRank = " + myGroupRank + " groupSize = " + 
                    groupSize + ", init done");
        }

        groupInit();
    }

    /**
     * This method gets called when the group is complete. The user can redefine
     * it to implement any initialization that depends on the rank of the
     * object or the size of the group.
     */
    public void groupInit() {
        if (Group.DEBUG) {
            logger.debug(Group._rank + ": GroupMember.groupInit()");
        }
    }
}

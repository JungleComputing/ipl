package ibis.group;

import ibis.ipl.*;
import java.util.StringTokenizer;
import java.util.Vector;

public class GroupMember { 

    // These are always filled correctly.
    public int groupID;           // groupID
    protected long myID;          // memberID of this object

    public long [] memberIDs;    // memberIDs of all members
    public int  [] memberRanks;  // cpu ranks of all members

    public int [] multicastHosts;
    public String multicastHostsID;

    protected String [] groupInterfaces;

    public  int rank;           // rank of this object 
    public  int size;           // size of the group

    public GroupSkeleton skeleton;

    public GroupMember() { 		

	if (Group.DEBUG) System.out.println("GroupMember() starting");

	try { 
	    String my_package = "";

	    Class myClass = this.getClass();

	    String temp = myClass.getName();
	    StringTokenizer s = new StringTokenizer(temp, ".");
	    
	    int tokens = s.countTokens();
	    
	    for (int i=0;i<tokens-1;i++) { 
		my_package += s.nextToken() + ".";
	    } 		
	    
	    String my_name = s.nextToken();

	    if (Group.DEBUG) {
		System.out.println("GroupMember() my type is " + my_package + my_name);
	    }

	    skeleton = (GroupSkeleton) Class.forName(my_package + "group_skeleton_" + my_name).newInstance();	
	    myID = Group.getNewGroupObjectID(skeleton);
	    
	    if (Group.DEBUG) {
		System.out.println("GroupMember() ID is " + myID);
	    }

	    Vector group_interfaces = new Vector();

	    Class tempClass = myClass;

	    while (tempClass != null) {
		Class [] interfaces = tempClass.getInterfaces(); 

		for (int i=0;i<interfaces.length;i++) { 					
		    if (isGroupInterface(interfaces[i]) && !group_interfaces.contains(interfaces[i])) {
			group_interfaces.add(interfaces[i]);
		    }
		}

		tempClass = tempClass.getSuperclass();
	    } 

	    groupInterfaces = new String[group_interfaces.size()];

	    if (Group.DEBUG) {
		System.out.print("GroupMember type " + myClass.getName() + " implements the group interfaces : "); 
	    }

	    for (int i=0;i<group_interfaces.size();i++) { 			
		groupInterfaces[i] = ((Class) group_interfaces.get(i)).getName();
		if (Group.DEBUG) {
		    System.out.print(groupInterfaces[i] + " ");
		}
	    }

	    if (Group.DEBUG) {
		System.out.println();
	    }
	} catch (Exception e) { 
	    if (Group.DEBUG) {
		System.out.println(" not found");
	    }
	    System.out.println("GroupMember could not init " + e);
	    System.exit(1);
	}	

	if (Group.DEBUG) System.out.println("GroupMember() done");
    }

    private boolean isGroupInterface(Class inter) { 

	if (inter == ibis.group.GroupInterface.class) { 
	    return true;
	}

	Class [] parents = inter.getInterfaces();

	for (int i=0;i<parents.length;i++) { 
	    if (isGroupInterface(parents[i])) { 
		return true;
	    }
	}

	return false;
    } 

    public int getRank() { 
	return rank;
    } 

    public int getSize() { 
	return size;
    } 

    protected void init(int groupNumber, long [] ids) { 			

	if (Group.DEBUG) {
	    System.out.println("GroupMember.init() starting");
	}
	
	groupID    = groupNumber;
	memberIDs  = ids;
	size       = ids.length;

	memberRanks = new int[size];
	multicastHosts = new int[size];

	for (int i=0;i<size;i++) { 
	    if (memberIDs[i] == myID) { 
		rank = i;
		System.out.print("*");
	    } 
	    multicastHosts[i] = memberRanks[i] = (int)((memberIDs[i] >> 32) & 0xFFFFFFFFL);

	    if (Group.DEBUG) {
		System.out.println("GroupMember " + i + " is on machine " + memberRanks[i]);
	    }
	}	       

	// sort multicastranks low...high (bubble sort)
	for (int i=0;i<multicastHosts.length-1;i++) { 
	    for (int j=i+1;j<multicastHosts.length;j++) { 
		if (multicastHosts[i] > multicastHosts[j]) { 
		    int temp = multicastHosts[i];
		    multicastHosts[i] = multicastHosts[j];
		    multicastHosts[j] = temp;
		} 
	    }		
	}

	// create a multicast ID
	StringBuffer buf = new StringBuffer("");

	for (int i=0;i<multicastHosts.length;i++) { 
	    buf.append(multicastHosts[i]);
	    buf.append(".");				
	} 
	
	multicastHostsID = buf.toString();

	// init the skeleton
	skeleton.init(this);
	Group.registerGroupMember(groupID, skeleton);	
	
	if (Group.DEBUG) { 
	    System.out.println("GroupMember.init() rank = " + rank + " size = " + size);
	    System.out.println("GroupMember.init() done");
	} 

	groupInit();
    }

    public void groupInit() { 
	/* overloaded by subtypes if required */
	System.out.println("GroupMember.groupInit()");
    } 
} 

package ibis.group;

/**
 * The {@link CombinedInvocationInfo} class is used to collect some information
 * about a combined invocation. Although it is supposed to be not directly
 * accessible by the user, it is public, because it is used in the skeletons.
 */
public class CombinedInvocationInfo implements java.io.Serializable { 

    /**
     * Group on which a group method using this combined-invocation info is
     * invoked. 
     */
    int groupID;

    /**
     * Name of the group method.
     */
    String method;

    /**
     * Identifier of this combined invocation.
     */
    String name;

    /**
     * Invocation mode, see {@link InvocationScheme}.
     */
    int mode;

    /**
     * The number of nodes involved in this combined invocation.
     */
    public int numInvokers;

    /**
     * The global rank numbers of the participating nodes.
     */
    public int [] participating_cpus;

    /**
     * Identification of where the reply is to be sent.
     */
    public int [] stubids_tickets;

    /**
     * Holds the input parameters of the combined invocation. There is
     * an entry for each invoker.
     */
    transient ParameterVector [] in;

    /**
     * Holds the result of the parameter combining. These are the actual
     * parameters to the underlying group invocation.
     */
    transient ParameterVector out;

    /**
     * Rank number of this node within the nodes involved in the combined
     * invocation. Note that this number has nothing to do with a rank number
     * within a group. It can be used as an index in the {@link #in},
     * {@link #stubids_tickets} or the {@link #participating_cpus} arrays.
     */
    transient int myInvokerRank;

    transient int present;

    /**
     * Constructor.
     *
     * @param groupID the group identification
     * @param method the name of the method
     * @param name the name of this combined invocation
     * @param mode the represents the invocation mode
     * @param size the number of nodes involved in this combined invocation
     */
    CombinedInvocationInfo(int groupID, String method, String name, int mode, int size) { 
	this.groupID = groupID;
	this.method = method;
	this.name = name;
	this.mode = mode;
	this.numInvokers = size;
	present = 0;
	participating_cpus = new int[numInvokers];			
	stubids_tickets = new int[numInvokers];
	for (int i=0;i<numInvokers;i++) { 
	    participating_cpus[i] = -1;
	} 
    } 	

    /**
     * When a group method is configured using a combined invocation scheme,
     * the configuration itself is a combined invocation. The
     * {@link GroupMethod#configure} call only returns when all invokers
     * are done. The group registry is used for this, using this method.
     *
     * @param rank the rank number within the nodes involved in the combined invocation
     * @param cpu the node number within the global group
     * @param stubId the identification of the stub
     */
    synchronized void addAndWaitUntilFull(int rank, int cpu) { 
        if (participating_cpus[rank] != -1) { 
	    throw new RuntimeException("Jikes !! Combined invocation rank handed out twice !!!");
        } 
        present++;
        participating_cpus[rank] = cpu;
        if (present < numInvokers) {
	    while (present < numInvokers) {
		try {
		    wait();
		} catch(Exception e) {
		    // ignore
		}
	    }
        }
        else {
	    notifyAll();
        }
    } 
} 

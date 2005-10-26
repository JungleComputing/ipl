/* $Id$ */

package ibis.gmi;

import java.util.Arrays;

import ibis.ipl.SendPort;

/**
 * A {@link GroupMethod} object represents a method of a specific group
 * reference. It contains a single method {@link #configure configure}
 * that can be used to set the invocation and reply handling schemes of
 * the method it represents.
 */
public class GroupMethod {
    /** 
     * After configuration, the {@link InvocationScheme} inv holds the
     * invocation scheme.
     */
    public InvocationScheme inv;

    /**
     * After configuration, the {@link ReplyScheme} rep holds the reply scheme.
     */
    public ReplyScheme rep;

    /** A summary of the invocation scheme. */
    public int invocation_mode;

    /** A summary of the reply scheme. */
    public int result_mode;

    /**
     * The sendport from which "single" or "group" invocations should obtain
     * the write message.
     */
    public SendPort sendport;

    /** A number uniquely defining this method. */
    public int index;

    /**
     * An identification of the destination skeleton, for single invocations.
     */
    public int destinationSkeleton;

    /**
     * A descriptor of the method, such as "int beep(java.lang.String,int[])".
     */
    String descriptor;

    /** A reference to the stub that could invoke, a.o., this method. */
    GroupStub parent_stub;

    /**
     * Additional information for a combined invocation, see
     * {@link CombinedInvocationInfo}.
     */
    CombinedInvocationInfo info;

    /**
     * A parameter vector specific for this method. It has methods to create
     * new parameter vectors or read parameter vectors of the proper type.
     */
    ParameterVector parameters;

    /**
     * Constructor.
     *
     * @param parent the parent stub
     * @param index the method identification
     * @param params a parameter vector specific for this method
     * @param descriptor the descriptor of the method
     */
    public GroupMethod(GroupStub parent, int index, ParameterVector params,
            String descriptor) {
        this.descriptor = descriptor;
        this.parent_stub = parent;
        this.index = index;
        this.parameters = params;
    }

    /**
     * Method configuration.
     * This method just fills in the fields, and does some sanity checks.
     *
     * @param invscheme the invocation scheme
     * @param repscheme the reply scheme
     *
     * @exception ConfigurationException when some illegal configuration
     * is given.
     */
    public void configure(InvocationScheme invscheme, ReplyScheme repscheme)
            throws ConfigurationException {
        
        switch (invscheme.mode) {
        case InvocationScheme.I_SINGLE:
            
            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting invocation mode to SINGLE");
                
            }
            
            SingleInvocation si = (SingleInvocation) invscheme;

            if (si.destination >= parent_stub.targetGroupSize
                    || si.destination < 0) {
                throw new ConfigurationException(
                        "Invalid configuration: destination groupmember ouf of "
                        + "range!");
            }

            destinationSkeleton = parent_stub.memberSkels[si.destination];
            sendport = Group.unicast[parent_stub.memberRanks[si.destination]];

            if (repscheme.mode == ReplyScheme.R_RETURN) {
                if (si.destination != ((ReturnReply) repscheme).rank) {
                    throw new ConfigurationException(
                            "Invalid configuration: invalid reply rank!");
                }
            }

            if (repscheme.mode == ReplyScheme.R_COMBINE_FLAT
                    || repscheme.mode == ReplyScheme.R_COMBINE_BINOMIAL) {
                throw new ConfigurationException(
                        "Invalid configuration: combined reply not possible "
                        + "with single invocation");
            }

            invocation_mode = invscheme.mode;
            break;

        case InvocationScheme.I_GROUP:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting invocation mode to GROUP");
                
            }
            
            invocation_mode = invscheme.mode;

            int [] targets = ((GroupInvocation) invscheme).targets;
            
            if (targets != null) { 
            
                int [] multicastHosts = new int[targets.length];
                boolean [] isset = new boolean[targets.length];
                
                Arrays.fill(isset, false);
                
                for (int i=0;i<targets.length;i++) { 
                    
                    int t = targets[i];
                    
                    if (t < 0 || t >= parent_stub.memberRanks.length) { 
                        throw new ConfigurationException(
                                "Invalid configuration: invocation target " + t  
                                + " out of range");                    
                    }
                    
                    // TODO: Maybe we can allow this ?
                    if (isset[t]) { 
                        throw new ConfigurationException(
                                "Invalid configuration: invocation target " + t  
                                + " used more that once");
                    }                
                    
                    multicastHosts[i] = parent_stub.memberRanks[t];                
                }
                
                // Now sort the target hosts in ascending order ....
                Arrays.sort(multicastHosts);
                
                // ... and generate the multicast ID
                StringBuffer buf = new StringBuffer("");

                for (int i = 0; i < multicastHosts.length; i++) {
                    buf.append(multicastHosts[i]);
                    
                    if (i != multicastHosts.length-1) { 
                        buf.append(".");
                    } 
                }

                String multicastHostsID = buf.toString();
                
                // Finally, create the multicast sendport
                sendport = MulticastGroups.getMulticastSendport(
                        multicastHostsID, multicastHosts);
                                
            } else {             
                // send to all in group, so use the pre-defined multicast id                 
                sendport = MulticastGroups.getMulticastSendport(
                        parent_stub.multicastHostsID,
                        parent_stub.multicastHosts);
            }
            
            break;

        case InvocationScheme.I_PERSONAL:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting invocation mode to PERSONAL");
                
            }
            
            invocation_mode = invscheme.mode;
            break;

        case InvocationScheme.I_COMBINED_FLAT:            
        case InvocationScheme.I_COMBINED_BINOMIAL: {

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting invocation mode to COMBINED");
                
            }
            
            CombinedInvocation ci = (CombinedInvocation) invscheme;

            configure(ci.inv, repscheme);

            invocation_mode = invscheme.mode + ci.inv.mode;
            info = Group.defineCombinedInvocation(ci, parent_stub.groupID,
                    descriptor, ci.id, invscheme.mode, ci.rank, ci.size);
            info.myInvokerRank = ci.rank;
        }
            break;
        }

        switch (repscheme.mode) {
        case ReplyScheme.R_DISCARD:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to DISCARD");
                
            }
            
            result_mode = repscheme.mode;
            break;
        case ReplyScheme.R_RETURN:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to RETURN");
                
            }

            result_mode = repscheme.mode;
            break;
        case ReplyScheme.R_FORWARD:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to FORWARD");
                
            }

            // Could check here that the forwarder supplied in fact has
            // a proper forward method for the result type of this group method!
            result_mode = repscheme.mode;
            break;
        case ReplyScheme.R_COMBINE_BINOMIAL:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to COMBINE_BINOMIAL");
                
            }

            result_mode = repscheme.mode;
            // Could check here that the combiner supplied in fact has
            // a proper combine method for the result type of this group method!
            break;
        case ReplyScheme.R_COMBINE_FLAT:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to COMBINE_FLAT");
                
            }

            // Could check here that the combiner supplied in fact has
            // a proper combine method for the result type of this group method!
            result_mode = repscheme.mode;
            break;
        case ReplyScheme.R_PERSONALIZED:

            if (Group.logger.isDebugEnabled()) { 
                Group.logger.debug(Group._rank
                        + ": GroupMethod.configure(...) - "
                        + " Setting reply mode to PERSONALIZED");
                
            }

            // No checks here, the default reply personalizer could be
            // fine.
            result_mode = repscheme.mode
                    + ((PersonalizeReply) repscheme).rs.mode;
            break;
        }

        this.inv = invscheme;
        this.rep = repscheme;

        if (invscheme.mode == InvocationScheme.I_COMBINED_FLAT
                || invscheme.mode == InvocationScheme.I_COMBINED_BINOMIAL) {
            CombinedInvocation ci = (CombinedInvocation) invscheme;
            Group.barrier(descriptor + parent_stub.groupID, ci.rank, ci.size);
        }
    }
}

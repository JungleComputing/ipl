/* $Id$ */

package ibis.gmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;

/**
 * Handler class for all upcalls.
 */
final class GroupCallHandler implements GroupProtocol, Upcall {

    /**
     * Invocation handler.
     *
     * @param r  the message to be read
     */
    private void handleInvocation(ReadMessage r) throws Exception {

        int dest = r.readInt();
        int inv = r.readByte();
        int res = r.readByte();

        switch (inv) {
        case InvocationScheme.I_GROUP:
        case InvocationScheme.I_COMBINED_FLAT_GROUP: {
            GroupSkeleton s = Group.getSkeletonByGroupID(dest);

            if (Group.logger.isDebugEnabled()) {
                Group.logger.debug(Group._rank + 
                        ": GroupCallHandler.handleInvocation() - "
                        + "It is a GROUP INVOCATION");
                Group.logger.debug(Group._rank + ": skeleton = " + s);
            }

            s.handleMessage(inv, res, r);
        }
            break;
        default: {
            GroupSkeleton s = Group.getSkeleton(dest);
            s.handleMessage(inv, res, r);
        }
            break;
        }
    }

    /**
     * Upcall method. Handle the upcall, divide and conquer.
     *
     * @param m  the message to be read.
     */
    public void upcall(ReadMessage m) {

        int ticket;
        GroupSkeleton s;
        byte opcode;
        byte resultMode;

        try {
            opcode = m.readByte();

            switch (opcode) {
            case GroupProtocol.REGISTRY:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a REGISTRY");
                }

                Group.registry.handleMessage(m);
                break;

            case GroupProtocol.INVOCATION:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got an INVOCATION");
                }

                handleInvocation(m);
                break;

            case GroupProtocol.REGISTRY_REPLY:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank
                            + ": GroupCallHandler.upcall() - "
                            + "Got a REGISTRY_REPLY");
                }

                ticket = m.readInt();
                RegistryReply r = (RegistryReply) m.readObject();

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - " +
                            "REGISTRY_REPLY forwarded to ticketMaster (" +
                            ticket + ")");
                }

                m.finish(); // ticketMaster may block.
                Group.logger.debug(Group._rank
                        + ": REGISTRY_REPLY forwarded to ticketMaster ("
                        + ticket + ")");
                Group.ticketMaster.put(ticket, r);
                break;

            case GroupProtocol.INVOCATION_FLATCOMBINE:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a INVOCATION_FLATCOMBINE");
                }

                Group.getGroupStub(m.readInt()).handleFlatInvocationCombineMessage(
                        m);
                break;

            case GroupProtocol.INVOCATION_BINCOMBINE:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - " +
                            "Got a INVOCATION_BINCOMBINE");
                }

                Group.getGroupStub(m.readInt()).handleBinInvocationCombineMessage(
                        m);
                break;

            case GroupProtocol.INVOCATION_REPLY:
                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - " +
                            "Got a INVOCATION_REPLY");
                }

                resultMode = m.readByte();
                ticket = m.readInt();
                int stub = ticket >> 16;
                ticket = ticket & 0xFFFF;
                
                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - " +
                            "INVOCATION_REPLY forwarded to stub (" + 
                            stub + ", " + ticket + ")");
                }

                Group.getGroupStub(stub).handleResultMessage(m,
                        ticket, resultMode);
                break;

            case GroupProtocol.COMBINE:
                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a COMBINE");
                }

                s = Group.getSkeleton(m.readInt());
                s.handleCombineMessage(m);
                break;

            case GroupProtocol.COMBINE_RESULT:

                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a COMBINE_RESULT");
                }
                
                s = Group.getSkeletonByGroupID(m.readInt());
                s.handleCombineMessage(m);
                break;
                
            case GroupProtocol.CREATE_MULTICAST_PORT:
                
                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a CREATE_MULTICAST_PORT");
                }
                
                MulticastGroups.handleCreateMulticastReceivePort(m);
                break;                
                
            case GroupProtocol.CREATE_MULTICAST_PORT_REPLY:
                
                if (Group.logger.isDebugEnabled()) {
                    Group.logger.debug(Group._rank + 
                            ": GroupCallHandler.upcall() - "
                            + "Got a CREATE_MULTICAST_PORT_REPLY");
                }
                                
                MulticastGroups.handleCreateMulticastReceivePortReply(m);
                break;                
                                
            default:                    
                Group.logger.warn(Group._rank + 
                        ": GroupCallHandler.upcall() - "
                        + "Got an illegal opcode !");
            }
        } catch (Exception e) {
            Group.logger.warn(Group._rank + 
                    ": GroupCallHandler.upcall() - " +
                    ": Got an exception in GroupCallHandler !", e);                
        }
    }
}

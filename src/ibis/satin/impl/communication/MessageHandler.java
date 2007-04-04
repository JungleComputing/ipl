/* $Id$ */

package ibis.satin.impl.communication;

import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;

import java.io.IOException;

public final class MessageHandler implements MessageUpcall, Protocol, Config {
    private Satin s;

    public MessageHandler(Satin s) {
        this.s = s;
    }

    public void upcall(ReadMessage m) throws IOException {
        SendPortIdentifier ident = m.origin();

        try {
            byte opcode = m.readByte();

            switch (opcode) {
            case EXIT:
                s.comm.handleExitMessage(ident.ibis());
                break;
            case EXIT_STAGE2:
                s.comm.handleExitStageTwoMessage(ident.ibis());
                break;
            case BARRIER_REQUEST:
                s.comm.handleBarrierRequestMessage();
                break;
            case EXIT_REPLY:
                s.comm.handleExitReply(m);
                break;
            case STEAL_AND_TABLE_REQUEST:
            case ASYNC_STEAL_AND_TABLE_REQUEST:
            case STEAL_REQUEST:
            case ASYNC_STEAL_REQUEST:
            case BLOCKING_STEAL_REQUEST:
                if(QUEUE_STEALS) {
                    s.lb.queueStealRequest(ident, opcode);
                } else {
                    m.finish(); // must finish, we will send back a reply.
                    s.lb.handleStealRequest(ident, opcode);
                }
                break;
            case STEAL_REPLY_FAILED:
            case STEAL_REPLY_SUCCESS:
            case ASYNC_STEAL_REPLY_FAILED:
            case ASYNC_STEAL_REPLY_SUCCESS:
            case STEAL_REPLY_FAILED_TABLE:
            case STEAL_REPLY_SUCCESS_TABLE:
            case ASYNC_STEAL_REPLY_FAILED_TABLE:
            case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
                s.lb.handleReply(m, opcode);
                break;
            case JOB_RESULT_NORMAL:
            case JOB_RESULT_EXCEPTION:
                s.lb.handleJobResult(m, opcode);
                break;
            case ABORT:
                s.aborts.handleAbort(m);
                break;
            case ABORT_AND_STORE:
                s.ft.handleAbortAndStore(m);
                break;
            case RESULT_REQUEST:
                s.ft.handleResultRequest(m);
                break;
            case RESULT_PUSH:
                s.ft.handleResultPush(m);
                break;
            case SO_REQUEST:
                s.so.handleSORequest(m, false);
                break;
            case SO_DEMAND:
                s.so.handleSORequest(m, true);
                break;
            case SO_TRANSFER:
                s.so.handleSOTransfer(m);
                break;
            case SO_NACK:
                s.so.handleSONack(m);
                break;
            case BARRIER_REPLY:
                s.comm.handleBarrierReply(ident.ibis());
                break;
            case GRT_UPDATE:
                s.ft.handleGRTUpdate(m);
                break;
            default:
                commLogger.error("SATIN '" + s.ident + "': Illegal opcode "
                    + opcode + " in MessageHandler");
            }
        } catch (IOException e) {
            commLogger.warn("satin msgHandler upcall for " + ident.ibis() + ": " + e, e);
            // Ignore.
            throw e;
        }
    }
}

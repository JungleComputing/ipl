/* $Id$ */

package ibis.satin.impl.communication;

public interface Protocol {
    static final byte EXIT = 1;

    static final byte EXIT_REPLY = 2;

    static final byte BARRIER_REPLY = 3;

    static final byte STEAL_REQUEST = 4;

    static final byte STEAL_REPLY_FAILED = 5;

    static final byte STEAL_REPLY_SUCCESS = 6;

    static final byte ASYNC_STEAL_REQUEST = 7;

    static final byte ASYNC_STEAL_REPLY_FAILED = 8;

    static final byte ASYNC_STEAL_REPLY_SUCCESS = 9;

    static final byte JOB_RESULT_NORMAL = 10;

    static final byte JOB_RESULT_EXCEPTION = 11;

    static final byte ABORT = 12;

    static final byte BLOCKING_STEAL_REQUEST = 15;

    static final byte CRASH = 16;

    static final byte ABORT_AND_STORE = 17;

    static final byte RESULT_REQUEST = 18;

    static final byte STEAL_AND_TABLE_REQUEST = 19;

    static final byte ASYNC_STEAL_AND_TABLE_REQUEST = 20;

    static final byte STEAL_REPLY_FAILED_TABLE = 21;

    static final byte STEAL_REPLY_SUCCESS_TABLE = 22;

    static final byte ASYNC_STEAL_REPLY_FAILED_TABLE = 23;

    static final byte ASYNC_STEAL_REPLY_SUCCESS_TABLE = 24;

    static final byte RESULT_PUSH = 25;

    static final byte SO_INVOCATION = 26;

    static final byte SO_REQUEST = 27;

    static final byte SO_TRANSFER = 28;

    static final byte EXIT_STAGE2 = 29;

    static final byte BARRIER_REQUEST = 30;
    
    static final byte GRT_UPDATE = 31;
}

package ibis.satin;

interface Protocol {
	static final byte EXIT =                      1;
	static final byte EXIT_REPLY =                2;
	static final byte BARRIER_REPLY =             3;
	static final byte STEAL_REQUEST =             4;
	static final byte STEAL_REPLY_FAILED =        5;
	static final byte STEAL_REPLY_SUCCESS =       6;
	static final byte ASYNC_STEAL_REQUEST =       7;
	static final byte ASYNC_STEAL_REPLY_FAILED =  8;
	static final byte ASYNC_STEAL_REPLY_SUCCESS = 9;
	static final byte JOB_RESULT =               10;
	static final byte ABORT =                    11;
	static final byte TUPLE_ADD =                12;
}

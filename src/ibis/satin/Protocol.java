package ibis.satin;

interface Protocol {
	static final byte EXIT =                1;
	static final byte BARRIER_REPLY =       2;
	static final byte STEAL_REQUEST =       4;
	static final byte STEAL_REPLY_FAILED =  5;
	static final byte STEAL_REPLY_SUCCESS = 6;
	static final byte JOB_RESULT =          7;
	static final byte ABORT =               8;
}

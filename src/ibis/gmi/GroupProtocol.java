package ibis.group;

public interface GroupProtocol { 

	/* ========== Major opcodes ========== */
	public static final byte 
		REGISTRY_REPLY   = 0,
		REGISTRY         = 1,
 	        INVOCATION       = 2,
	        BARRIER          = 3,
 	        COMBINE          = 4, 
		COMBINE_RESULT   = 5, 
		INVOCATION_REPLY = 6;

	/* ========== Minor opcodes =========== */ 
 
	// registy calls
	public static final byte
		CREATE_GROUP    = 0,
	        JOIN_GROUP      = 1, 		      
 	        BARRIER_GROUP   = 2,
 	        FIND_GROUP      = 3,
		DEFINE_COMBINED = 4;

        // registry replies
	public static final byte
		CREATE_OK       = 0, 
		CREATE_FAILED   = 1,  				      

		JOIN_OK         = 10,  
	        JOIN_UNKNOWN    = 11, 
		JOIN_FULL       = 12, 
		JOIN_WRONG_TYPE = 13, 

		BARRIER_OK      = 20, 
		BARRIER_FAILED  = 21, 

		GROUP_UNKOWN    = 30, 
		GROUP_OK        = 31, 
		GROUP_NOT_READY = 32,

		COMBINED_FAILED = 40,
		COMBINED_OK     = 41;
						
	// Invocation mode
	//	public static final byte
		//		LOCAL    = (0 << 2), 
		//		REMOTE   = (1 << 2), 
		//		GROUP    = (2 << 2), 
		//		PERSONAL = (3 << 2);

	// Result mode
	//	public static final byte
	//		DISCARD  = 0, 
	//		RETURN   = 1, 
	//		COMBINE  = 2;


	
	
} 

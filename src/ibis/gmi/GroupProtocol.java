package ibis.group;

public interface GroupProtocol { 

	/* ========== Major opcodes ========== */
	public static final byte 
		REPLY      = 0,
		REGISTRY   = 1,
 	        INVOCATION = 2,
	        BARRIER    = 3,
 	        COMBINE    = 4, 
		COMBINE_RESULT = 5;
	
	/* ========== Minor opcodes =========== */ 
 
	// registy calls
	public static final byte
		CREATE_GROUP  = 0,
	        JOIN_GROUP    = 1, 		      
 	        BARRIER_GROUP = 2;

        // registry replies
	public static final byte
		CREATE_OK      = 0, 
		CREATE_FAILED  = 1,  				      
		JOIN_OK        = 2,  
	        JOIN_UNKNOWN   = 3, 
		JOIN_FULL      = 4, 
		BARRIER_OK     = 5, 
		BARRIER_FAILED = 6;
				      
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

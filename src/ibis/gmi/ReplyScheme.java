package ibis.group;

public class ReplyScheme { 
    // base class for reply schemes	

    public static final int
        /* result modes */
	R_DISCARD                     = 0, 
	R_RETURN                      = 1, 
	R_FORWARD                     = 2, 
	R_COMBINE_FLAT                = 3, 
	R_COMBINE_BINARY              = 4, 
	R_PERSONALIZED                = 10,
	R_PERSONALIZED_RETURN         = R_PERSONALIZED + R_RETURN,
	R_PERSONALIZED_FORWARD        = R_PERSONALIZED + R_FORWARD, 
	R_PERSONALIZED_COMBINE_FLAT   = R_PERSONALIZED + R_COMBINE_FLAT, 
	R_PERSONALIZED_COMBINE_BINARY = R_PERSONALIZED + R_COMBINE_BINARY; 

    int mode;

    ReplyScheme(int mode) { 
	this.mode = mode;
    } 	
}

package ibis.group;

public class InvocationScheme { 
    // base class for invocation schemes

    /* invocation modes */
    public final static int 
	I_SINGLE                   = 0,
	I_GROUP                    = 1,
	I_PERSONAL                 = 2,
	I_COMBINED_FLAT            = 10,
	I_COMBINED_BINARY          = 20,

	I_COMBINED_FLAT_SINGLE     = I_COMBINED_FLAT + I_SINGLE,
	I_COMBINED_FLAT_GROUP      = I_COMBINED_FLAT + I_GROUP,
	I_COMBINED_FLAT_PERSONAL   = I_COMBINED_FLAT + I_PERSONAL, 

	I_COMBINED_BINARY_SINGLE   = I_COMBINED_BINARY + I_SINGLE,
	I_COMBINED_BINARY_GROUP    = I_COMBINED_BINARY + I_GROUP,
	I_COMBINED_BINARY_PERSONAL = I_COMBINED_BINARY + I_PERSONAL; 

    int mode;

    InvocationScheme(int mode) { 
	this.mode = mode;
    } 	
}

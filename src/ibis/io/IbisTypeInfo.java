package ibis.io;

class IbisTypeInfo implements IbisStreamFlags { 
    Class clazz;		
    boolean isArray;
    boolean isString;
    boolean isClass;

    static Class stringClass;
    static Class classClass;

    // for ibis.io.Serializable    
    Generator gen;

    // for java.io.Serializable
    AlternativeTypeInfo altInfo;

    IbisTypeInfo(Class clzz) {
	clazz = clzz;
	isArray = clazz.isArray();
	isString = (clazz == stringClass);
	isClass = (clazz == classClass);
	if (isArray || isString || isClass) {
	    gen = null;
	}
	else {
	    Class gen_class = null;
	    String name = clazz.getName() + "_ibis_io_Generator";
	    try {
		gen_class = IbisStreamTypes.doLoadClass(name);
	    } catch (ClassNotFoundException e) {
		if (DEBUG) {
		    System.out.println("Class " + name + " not found!");
		}
	    }
	    if (gen_class != null) {
		try {
		    gen = (Generator) gen_class.newInstance();
		} catch(Exception e) {
		    if (DEBUG) {
			System.out.println("Could not instantiate " + name);
		    }
		}
	    }
	}

	if (gen == null) { 
	    altInfo = new AlternativeTypeInfo(clazz);
	}	   
    } 

    static {
	try { 
	    stringClass = Class.forName("java.lang.String");
	} catch (Exception e) { 
	    System.err.println("Failed to find java.lang.String " + e);
	    System.exit(1);
	}
	try { 
	    classClass = Class.forName("java.lang.Class");
	} catch (Exception e) { 
	    System.err.println("Failed to find java.lang.Class " + e);
	    System.exit(1);
	}
    }
} 


package ibis.io;

class IbisTypeInfo implements IbisStreamFlags { 
    Class clazz;		
    boolean isArray;
    boolean isString;
    boolean isClass;

    // for ibis.io.Serializable    
    Generator gen;

    // for java.io.Serializable
    AlternativeTypeInfo altInfo;

    IbisTypeInfo(Class clzz) {
	clazz = clzz;
	isArray = clazz.isArray();
	isString = (clazz == classString);
	isClass = (clazz == classClass);
	if (isArray || isString || isClass) {
	    gen = null;
	}
	else {
	    Class gen_class = null;
	    String name = clazz.getName() + "_ibis_io_Generator";
	    try {
		gen_class = Class.forName(name);
	    } catch (ClassNotFoundException e) {
		// The loading of the class failed.
		// Maybe, Ibis was loaded using the primordial classloader
		// and the needed class was not.
		try {
		    gen_class = Thread.currentThread().getContextClassLoader()
				.loadClass(name);
		} catch(ClassNotFoundException e1) {
		    if (DEBUG) {
			System.out.println("Class " + name + " not found!");
		    }
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
} 

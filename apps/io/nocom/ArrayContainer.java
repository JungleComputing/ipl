public final class ArrayContainer implements TestObject {
    public static final int BYTE   = 1;
    public static final int INT    = 2;
    public static final int LONG   = 3;
    public static final int FLOAT  = 4;
    public static final int DOUBLE = 5;

    int type;
    int len;
    Object array;

    public ArrayContainer(int type, int size) {
	switch(type) {
	default:
	    len = size;
	    array = new byte[len];
	    break;
	case INT:
	    len = size/4;
	    array = new int[len];
	    break;
	case LONG:
	    len = size/8;
	    array = new long[len];
	    break;
	case FLOAT:
	    len = size/4;
	    array = new float[len];
	    break;
	case DOUBLE:
	    len = size/8;
	    array = new double[len];
	    break;
	}
	this.type = type;
	this.len = len;
    }

    public int object_size() {
	return payload();
    }

    public int payload() {
	switch(type) {
	case INT:
	    return len * 4;
	case LONG:
	    return len * 8;
	case FLOAT:
	    return len * 4;
	case DOUBLE:
	    return len * 8;
	}
	return len;
    }

    public int num_objs() {
	return 1;
    }

    public String id() {
	String val = "byte";
	switch(type) {
	case INT:
	    val = "int";
	    break;
	case LONG:
	    val = "long";
	    break;
	case FLOAT:
	    val = "float";
	    break;
	case DOUBLE:
	    val = "double";
	    break;
	}
	return val + " array of length " + len;
    }
}

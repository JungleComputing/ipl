package ibis.io;

interface IbisStreamFlags extends TypeSize { 
    public static final boolean DEBUG = false;
    public static final boolean ASSERTS = true;

    public static final int BUFFER_SIZE = 2*1024;
    public static final int ARRAY_BUFFER_SIZE	= 1024;


    public static final int TYPE_BIT   = (1 << 31);
    public static final int TYPE_MASK = ~TYPE_BIT;

    public static final int NUL_HANDLE = 0;
    public static final int RESET_HANDLE = 1;
    public static final int CONTROL_HANDLES = 2;

    static final Class classBooleanArray = IbisStreamTypes.arrayClass("[Z");
    static final Class classByteArray    = IbisStreamTypes.arrayClass("[B");
    static final Class classShortArray   = IbisStreamTypes.arrayClass("[S");
    static final Class classCharArray    = IbisStreamTypes.arrayClass("[C");
    static final Class classIntArray     = IbisStreamTypes.arrayClass("[I");
    static final Class classLongArray    = IbisStreamTypes.arrayClass("[J");
    static final Class classFloatArray   = IbisStreamTypes.arrayClass("[F");
    static final Class classDoubleArray  = IbisStreamTypes.arrayClass("[D");

    /* This array can be indexed using a TYPE_XXX variable */
    static final Class[] arrayClasses = {null, classBooleanArray, classByteArray, 
					 classCharArray, classShortArray, classIntArray, 
					 classLongArray, classFloatArray, classDoubleArray, 
					 null, null};

    static final Class classBoolean      = classBooleanArray.getComponentType();
    static final Class classByte         = classByteArray.getComponentType();
    static final Class classShort        = classShortArray.getComponentType();
    static final Class classChar         = classCharArray.getComponentType();
    static final Class classInt          = classIntArray.getComponentType();
    static final Class classLong         = classLongArray.getComponentType();
    static final Class classFloat        = classFloatArray.getComponentType();
    static final Class classDouble       = classDoubleArray.getComponentType();

    public static final int BOOLEAN_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_BOOLEAN;
    public static final int BYTE_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_BYTE;
    public static final int CHAR_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_CHAR;
    public static final int SHORT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_SHORT;
    public static final int INT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_INT;
    public static final int LONG_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_LONG;
    public static final int FLOAT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_FLOAT;
    public static final int DOUBLE_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_DOUBLE;
    public static final int REF_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_INT;

    public static final int HANDLE_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_INT;
} 


package ibis.io;

public interface IbisStreamFlags { 
    public static final boolean DEBUG = false;
    public static final boolean ASSERTS = false;

    public static final int BUFFER_SIZE = 2*1024;
    public static final int ARRAY_BUFFER_SIZE	= 1024;

    public static final int TYPE_BIT   = (1 << 31);
    public static final int TYPE_MASK = ~TYPE_BIT;

    public static final int NUL_HANDLE = 0;
    public static final int RESET_HANDLE = 1;
    public static final int CONTROL_HANDLES = 2;

    static final Class classBooleanArray = IbisTypeInfo.getClass("[Z");
    static final Class classByteArray    = IbisTypeInfo.getClass("[B");
    static final Class classShortArray   = IbisTypeInfo.getClass("[S");
    static final Class classCharArray    = IbisTypeInfo.getClass("[C");
    static final Class classIntArray     = IbisTypeInfo.getClass("[I");
    static final Class classLongArray    = IbisTypeInfo.getClass("[J");
    static final Class classFloatArray   = IbisTypeInfo.getClass("[F");
    static final Class classDoubleArray  = IbisTypeInfo.getClass("[D");

    static final int	SIZEOF_BOOLEAN	= 1;
    static final int	SIZEOF_BYTE	= 1;
    static final int	SIZEOF_CHAR	= 2;
    static final int	SIZEOF_SHORT	= 2;
    static final int	SIZEOF_INT	= 4;
    static final int	SIZEOF_LONG	= 8;
    static final int	SIZEOF_FLOAT	= 4;
    static final int	SIZEOF_DOUBLE	= 8;

    static final int	TYPE_BOOLEAN	= 1;
    static final int	TYPE_BYTE	= 2;
    static final int	TYPE_CHAR	= 3;
    static final int	TYPE_SHORT	= 4;
    static final int	TYPE_INT	= 5;
    static final int	TYPE_LONG	= 6;
    static final int	TYPE_FLOAT	= 7;
    static final int	TYPE_DOUBLE	= 8;

    static final int	BEGIN_TYPES	= 1;
    static final int	PRIMITIVE_TYPES	= 9;

    public static final int BOOLEAN_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_BOOLEAN;
    public static final int BYTE_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_BYTE;
    public static final int CHAR_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_CHAR;
    public static final int SHORT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_SHORT;
    public static final int INT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_INT;
    public static final int LONG_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_LONG;
    public static final int FLOAT_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_FLOAT;
    public static final int DOUBLE_BUFFER_SIZE	= BUFFER_SIZE / SIZEOF_DOUBLE;
} 

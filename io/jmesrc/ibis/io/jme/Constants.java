package ibis.io.jme;

public interface Constants {
    public static final int TYPE_BIT = (1 << 31);

    public static final int TYPE_MASK = ~TYPE_BIT;

    public static final int NUL_HANDLE = 0;

    public static final int RESET_HANDLE = 1;

    public static final int CLEAR_HANDLE = -1;

    public static final int CONTROL_HANDLES = 2;

    static final Class classBooleanArray = AlternativeTypeInfo.getClass("[Z");

    static final Class classByteArray = AlternativeTypeInfo.getClass("[B");

    static final Class classShortArray = AlternativeTypeInfo.getClass("[S");

    static final Class classCharArray = AlternativeTypeInfo.getClass("[C");

    static final Class classIntArray = AlternativeTypeInfo.getClass("[I");

    static final Class classLongArray = AlternativeTypeInfo.getClass("[J");

    static final Class classFloatArray = AlternativeTypeInfo.getClass("[F");

    static final Class classDoubleArray = AlternativeTypeInfo.getClass("[D");

    static final int SIZEOF_BOOLEAN = 1;

    static final int SIZEOF_BYTE = 1;

    static final int SIZEOF_CHAR = 2;

    static final int SIZEOF_SHORT = 2;

    static final int SIZEOF_INT = 4;

    static final int SIZEOF_LONG = 8;

    static final int SIZEOF_FLOAT = 4;

    static final int SIZEOF_DOUBLE = 8;

    static final int TYPE_BOOLEAN = 1;

    static final int TYPE_BYTE = 2;

    static final int TYPE_CHAR = 3;

    static final int TYPE_SHORT = 4;

    static final int TYPE_INT = 5;

    static final int TYPE_LONG = 6;

    static final int TYPE_FLOAT = 7;

    static final int TYPE_DOUBLE = 8;

    static final int BEGIN_TYPES = 1;

    static final int PRIMITIVE_TYPES = 9;
}

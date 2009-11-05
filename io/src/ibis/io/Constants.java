package ibis.io;

/**
 * Some constants for the {@code ibis.io} package.
 * This class must be public because it is accessed by Ibisc-rewritten code.
 */
public final class Constants {
    public static final int TYPE_BIT = (1 << 31);

    public static final int TYPE_MASK = ~TYPE_BIT;

    public static final int NUL_HANDLE = 0;

    public static final int RESET_HANDLE = 1;

    public static final int CLEAR_HANDLE = -1;

    public static final int CONTROL_HANDLES = 2;

    public static final Class<?> classBooleanArray = AlternativeTypeInfo.getClass("[Z");

    public static final Class<?> classByteArray = AlternativeTypeInfo.getClass("[B");

    public static final Class<?> classShortArray = AlternativeTypeInfo.getClass("[S");

    public static final Class<?> classCharArray = AlternativeTypeInfo.getClass("[C");

    public static final Class<?> classIntArray = AlternativeTypeInfo.getClass("[I");

    public static final Class<?> classLongArray = AlternativeTypeInfo.getClass("[J");

    public static final Class<?> classFloatArray = AlternativeTypeInfo.getClass("[F");

    public static final Class<?> classDoubleArray = AlternativeTypeInfo.getClass("[D");

    public static final int SIZEOF_BOOLEAN = 1;

    public static final int SIZEOF_BYTE = 1;

    public static final int SIZEOF_CHAR = 2;

    public static final int SIZEOF_SHORT = 2;

    public static final int SIZEOF_INT = 4;

    public static final int SIZEOF_LONG = 8;

    public static final int SIZEOF_FLOAT = 4;

    public static final int SIZEOF_DOUBLE = 8;

    public static final int TYPE_BOOLEAN = 1;

    public static final int TYPE_BYTE = 2;

    public static final int TYPE_CHAR = 3;

    public static final int TYPE_SHORT = 4;

    public static final int TYPE_INT = 5;

    public static final int TYPE_LONG = 6;

    public static final int TYPE_FLOAT = 7;

    public static final int TYPE_DOUBLE = 8;

    public static final int BEGIN_TYPES = 1;

    public static final int PRIMITIVE_TYPES = 9;
}

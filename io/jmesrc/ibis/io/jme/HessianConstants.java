package ibis.io.jme;

public interface HessianConstants {

	public static final int INT_BYTE_MIN = -0x10;
	public static final int INT_BYTE_MAX = 0x2f;
	public static final int INT_BYTE_ZERO = 0x90;
	public static final int INT_BYTE_ENCODED_MIN = INT_BYTE_MIN + INT_BYTE_ZERO;
	public static final int INT_BYTE_ENCODED_MAX = INT_BYTE_MAX + INT_BYTE_ZERO;

	public static final int INT_SHORT_MIN = -0x800;
	public static final int INT_SHORT_MAX = 0x7ff;
	public static final int INT_SHORT_ZERO = 0xc8;
	public static final int INT_SHORT_ENCODED_MIN = INT_SHORT_MIN + INT_SHORT_ZERO;
	public static final int INT_SHORT_ENCODED_MAX = INT_SHORT_MAX + INT_SHORT_ZERO;

	public static final int INT_TRIP_MIN = -0x40000;
	public static final int INT_TRIP_MAX = 0x3ffff;
	public static final int INT_TRIP_ZERO = 0xd4;
	public static final int INT_TRIP_ENCODED_MIN = INT_TRIP_MIN + INT_TRIP_ZERO;
	public static final int INT_TRIP_ENCODED_MAX = INT_TRIP_MAX + INT_TRIP_ZERO;

	public static final long LONG_BYTE_MIN = -0x08;
	public static final long LONG_BYTE_MAX = 0x0f;
	public static final int LONG_BYTE_ZERO = 0xe0;
	public static final long LONG_BYTE_ENCODED_MIN = LONG_BYTE_MIN + LONG_BYTE_ZERO;
	public static final long LONG_BYTE_ENCODED_MAX = LONG_BYTE_MAX + LONG_BYTE_ZERO;

	public static final long LONG_SHORT_MIN = -0x800;
	public static final long LONG_SHORT_MAX =  0x7ff;
	public static final int LONG_SHORT_ZERO = 0xf8;
	public static final long LONG_SHORT_ENCODED_MIN = LONG_BYTE_MIN + LONG_BYTE_ZERO;
	public static final long LONG_SHORT_ENCODED_MAX = LONG_BYTE_MAX + LONG_BYTE_ZERO;

	public static final int LONG_TRIP_MIN = -0x40000;
	public static final int LONG_TRIP_MAX = 0x3ffff;
	public static final int LONG_TRIP_ZERO = 0x3c;
	public static final int LONG_TRIP_ENCODED_MIN = LONG_TRIP_MIN + LONG_TRIP_ZERO;
	public static final int LONG_TRIP_ENCODED_MAX = LONG_TRIP_MAX + LONG_TRIP_ZERO;

	public static final int STRING_BYTE_MAX = 0x1f;
	public static final int STRING_BYTE_MIN = 0x00;
	public static final int STRING_CHUNK_SIZE = (2 * Short.MAX_VALUE) + 1;

	public static final int LONG_INT = 0x77;

	public static final int DOUBLE_ZERO = 0x67;
	public static final int DOUBLE_ONE = 0x68;
	public static final int DOUBLE_BYTE = 0x69;
	public static final int DOUBLE_SHORT = 0x6a;
	public static final int DOUBLE_FLOAT = 0x6b;
}

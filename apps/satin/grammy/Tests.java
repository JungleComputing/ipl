// File: $Id$

import junit.framework.TestCase;

public class Tests extends TestCase {
    final String sample = "A long long time ago in a galaxy far away";

    public void testDecodeByte()
    {
	assertEquals( Helpers.decodeByte( (byte) 0 ), 0 );
	assertEquals( Helpers.decodeByte( (byte) 1 ), 1 );
	assertEquals( Helpers.decodeByte( (byte) -1 ), 255 );
    }

    public void testDecodeShort()
    {
	assertEquals( Helpers.decodeShort( (byte) 0, (byte) 0 ), 0 );
	assertEquals( Helpers.decodeShort( (byte) 1, (byte) 0 ), 256 );
	assertEquals( Helpers.decodeShort( (byte) 1, (byte) 1 ), 257 );
	assertEquals( Helpers.decodeShort( (byte) -1, (byte) -1 ), 65535 );
    }
}

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

    public void testEncoding()
    {
	ByteBuffer buf = new ByteBuffer();
	//short codes[] = { 0, 128, 254, 255, 2000, -1 };
	short codes[] = { 0, 128, 254, 255, 2000, (short) 65530 };
	buf.append( codes );

	assertEquals( 12, buf.getLength() );
    }
}

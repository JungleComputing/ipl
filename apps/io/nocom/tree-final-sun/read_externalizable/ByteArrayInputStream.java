// A java.io.ByteArrayInputStream without all these synchronized()

/*
 * Java core library component.
 *
 * Copyright (c) 1997, 1998
 *      Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file.
 */
public
final		// Usually this is essential for (inlined) performance
class ByteArrayInputStream
  extends java.io.InputStream
{
	protected byte[] buf;
	protected int pos = 0;
	protected int count;
	protected int mark;
	private int off;

public ByteArrayInputStream(byte buffer[])
	{
	this(buffer, 0, buffer.length);
}

public ByteArrayInputStream(byte buffer[], int offset, int length)
	{
	count = length;
	off = offset;
	buf = buffer;
	mark = 0;
	pos = 0;
}

public int available()
	{
	return (count-pos);
}

public void mark(int markpos)
	{
	mark = pos;
	if (pos + markpos > buf.length) {
	    throw new RuntimeException("Insufficient buffer space");
	}
}

public boolean markSupported()
	{
	return (true);
}

public int read()
	{
	if (pos == count) {
		return (-1);
	}
	return (((int)buf[off + pos++]) & 0xFF);
}

public int read(byte b[], int offset, int len)
	{
	if (pos==count) {
		return (-1);
	}

	int toRead = Math.min(available(), len);
	System.arraycopy(buf, off+pos, b, offset, toRead);
	pos += toRead;

	return (toRead);
}

public void reset()
	{
	pos = mark;
}

public long skip(long n)
	{
	long toSkip = Math.min(n, available());
	pos += (int)toSkip;

	return (toSkip);
}
}

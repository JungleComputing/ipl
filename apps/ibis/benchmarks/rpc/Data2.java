// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 

import ibis.io.Generator;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import java.io.IOException;
import java.io.Serializable;

import ibis.io.MantaTypedBufferInputStream;
import ibis.io.MantaTypedBufferOutputStream;


final class Data2_ibis_io_Generator extends Generator
{

    public final Object generated_newInstance(MantaInputStream mantainputstream)
	throws IOException, ClassNotFoundException
    {
	return new Data2(mantainputstream);
    }

    public Data2_ibis_io_Generator()
    {
    }
}


// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DowncallLatency.java

final class Data2
    implements Serializable, ibis.io.Serializable
{

    Data2(int n)
    {
	i0 = fill++;
	i1 = fill++;
	i2 = fill++;
	i3 = fill++;
	int l = (n - 1) / 2;
	int r = n - 1 - l;
	if(l > 0)
	    left = new Data2(l);
	if(r > 0)
	    right = new Data2(r);
    }


    public String toString() {
      return "[" + i0 + "," + i1 + "," + i2 + "," + i3 + "](" + left + ";" + right + ")";
    }


    /* ---------- Use inline-expanded version above
    public final void generated_WriteObject(MantaOutputStream mantaoutputstream)
	throws IOException
    {
	MantaTypedBufferOutputStream o = (MantaTypedBufferOutputStream)mantaoutputstream;

	if (o.int_index + 4 >= o.INT_BUFFER_SIZE) {
	    o.flush(false);
	}
	o.int_buffer[o.int_index++] = i3;
	o.int_buffer[o.int_index++] = i2;
	o.int_buffer[o.int_index++] = i1;
	o.int_buffer[o.int_index++] = i0;

	int i = o.writeKnownObjectHeader(right);
	if(i == 1)
	    right.generated_WriteObject(mantaoutputstream);
	i = o.writeKnownObjectHeader(left);
	if(i == 1)
	    left.generated_WriteObject(mantaoutputstream);
    }

    public final void generated_ReadObject(MantaInputStream mantainputstream)
	throws IOException, ClassNotFoundException
    {
	i3 = mantainputstream.readInt();
	i2 = mantainputstream.readInt();
	i1 = mantainputstream.readInt();
	i0 = mantainputstream.readInt();
    }

    public Data2(MantaInputStream mantainputstream)
	throws IOException, ClassNotFoundException
    {
	MantaTypedBufferInputStream o = (MantaTypedBufferInputStream)mantainputstream;

	mantainputstream.addObjectToCycleCheck(this);
	if (o.int_index == o.max_int_index) {
	    o.receive();
	}
	i3 = o.int_buffer[o.int_index++];
	i2 = o.int_buffer[o.int_index++];
	i1 = o.int_buffer[o.int_index++];
	i0 = o.int_buffer[o.int_index++];

	int i = mantainputstream.readKnownTypeHeader();
	if(i == -1)
	    right = new Data2(mantainputstream);
	else
	if(i != 0)
	    right = (Data2)mantainputstream.getObjectFromCycleCheck(i);
	i = mantainputstream.readKnownTypeHeader();
	if(i == -1)
	    left = new Data2(mantainputstream);
	else
	if(i != 0)
	    left = (Data2)mantainputstream.getObjectFromCycleCheck(i);
    }
    ---------- Use inline-expanded version above */


    /*---------- Use inline-expanded version above */

    public final void generated_WriteObject(MantaOutputStream mantaoutputstream)
	throws IOException
    {
	mantaoutputstream.writeInt(i3);
	mantaoutputstream.writeInt(i2);
	mantaoutputstream.writeInt(i1);
	mantaoutputstream.writeInt(i0);
	int i = mantaoutputstream.writeKnownObjectHeader(right);
	if(i == 1)
	    right.generated_WriteObject(mantaoutputstream);
	i = mantaoutputstream.writeKnownObjectHeader(left);
	if(i == 1)
	    left.generated_WriteObject(mantaoutputstream);
    }

    public final void generated_ReadObject(MantaInputStream mantainputstream)
	throws IOException, ClassNotFoundException
    {
	i3 = mantainputstream.readInt();
	i2 = mantainputstream.readInt();
	i1 = mantainputstream.readInt();
	i0 = mantainputstream.readInt();
    }

    public Data2(MantaInputStream mantainputstream)
	throws IOException, ClassNotFoundException
    {
	mantainputstream.addObjectToCycleCheck(this);
	i3 = mantainputstream.readInt();
	i2 = mantainputstream.readInt();
	i1 = mantainputstream.readInt();
	i0 = mantainputstream.readInt();
	int i = mantainputstream.readKnownTypeHeader();
	if(i == -1)
	    right = new Data2(mantainputstream);
	else
	if(i != 0)
	    right = (Data2)mantainputstream.getObjectFromCycleCheck(i);
	i = mantainputstream.readKnownTypeHeader();
	if(i == -1)
	    left = new Data2(mantainputstream);
	else
	if(i != 0)
	    left = (Data2)mantainputstream.getObjectFromCycleCheck(i);
    }

    /* ---------- Use inline-expanded version above */


    static int fill;
    int i0;
    int i1;
    int i2;
    int i3;
    Data2 left;
    Data2 right;
}


/* Use the patched & decompiled version above -----

final class Data2 implements java.io.Serializable {

    int i0;
    int i1;
    int i2;
    int i3;

    static int        fill;

    Data2 left;
    Data2 right;

    Data2(int n) {
      i0 = fill++;
      i1 = fill++;
      i2 = fill++;
      i3 = fill++;
      int l = (n - 1) / 2;
      int r = n - 1 - l;
      if (l > 0) left = new Data2(l);
      if (r > 0) right = new Data2(r);
    }

}

 ------ Use the patched & decompiled version above */


import ibis.io.Generator;
import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import java.io.IOException;
import java.io.Serializable;


final class Data1_ibis_io_Generator extends Generator
{

    public final Object generated_newInstance(MantaInputStream mantainputstream)
        throws IOException, ClassNotFoundException
    {
        return new Data1(mantainputstream);
    }

    public Data1_ibis_io_Generator()
    {
    }
}


// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DowncallLatency.java

final class Data1
    implements Serializable, ibis.io.Serializable
{

    Data1()
    {
        i0 = fill++;
        i1 = fill++;
        i2 = fill++;
        i3 = fill++;
    }

    public final void generated_WriteObject(MantaOutputStream mantaoutputstream)
        throws IOException
    {
        mantaoutputstream.writeInt(i3);
        mantaoutputstream.writeInt(i2);
        mantaoutputstream.writeInt(i1);
        mantaoutputstream.writeInt(i0);
    }

    public final void generated_ReadObject(MantaInputStream mantainputstream)
        throws IOException, ClassNotFoundException
    {
        i3 = mantainputstream.readInt();
        i2 = mantainputstream.readInt();
        i1 = mantainputstream.readInt();
        i0 = mantainputstream.readInt();
    }

    public Data1(MantaInputStream mantainputstream)
        throws IOException, ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        i3 = mantainputstream.readInt();
        i2 = mantainputstream.readInt();
        i1 = mantainputstream.readInt();
        i0 = mantainputstream.readInt();
    }

    static int fill;
    int i0;
    int i1;
    int i2;
    int i3;
}


/* Use the patched & decompiled version above -----

final class Data1 implements java.io.Serializable // , ibis.io.Serializable
{

    int i0;
    int i1;
    int i2;
    int i3;

    static int	fill;

    Data1() {
	i0 = fill++;
	i1 = fill++;
	i2 = fill++;
	i3 = fill++;
    }

}

 ------ Use the patched & decompiled version above */

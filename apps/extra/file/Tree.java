// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Tree.java

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import java.io.Serializable;

public final class Tree
    implements Serializable, ibis.io.Serializable
{	
	public static int count = 0;

    public Tree(int j)
    {
	    i = count++;
	    i1 = i;
	    i2 = i;
	    i3 = i;

        int k = j / 2;
        if(k > 0)
            left = new Tree(k);
        if(j - k - 1 > 0)
            right = new Tree(j - k - 1);
    }

    public final void generated_WriteObject(MantaOutputStream mantaoutputstream)
        throws IbisIOException
    {
	    System.out.println("writing object " + i);

        mantaoutputstream.writeInt(i3);
        mantaoutputstream.writeInt(i2);
        mantaoutputstream.writeInt(i1);
        mantaoutputstream.writeInt(i);
        int j = mantaoutputstream.writeKnownObjectHeader(right);
        if(j == 1)
            right.generated_WriteObject(mantaoutputstream);
        j = mantaoutputstream.writeKnownObjectHeader(left);
        if(j == 1)
            left.generated_WriteObject(mantaoutputstream);
    }

    public Tree(MantaInputStream mantainputstream)
        throws IbisIOException
    {
        mantainputstream.addObjectToCycleCheck(this);
        i3 = mantainputstream.readInt();
        i2 = mantainputstream.readInt();
        i1 = mantainputstream.readInt();
        i = mantainputstream.readInt();

	System.out.println("read object " + i);

	if (i1 != i || i2 !=i|| i3 != i) { 
		System.out.println("Verification failed");
		System.out.println("i = " + i);
		System.out.println("i1 = " + i1);
		System.out.println("i2 = " + i2);
		System.out.println("i3 = " + i3);

		new Exception().printStackTrace();
		System.exit(1);		
	}


        int j = mantainputstream.readKnownTypeHeader();
        if(j == -1)
            right = new Tree(mantainputstream);
        else
        if(j != 0)
            right = (Tree)mantainputstream.getObjectFromCycleCheck(j);
        j = mantainputstream.readKnownTypeHeader();
        if(j == -1)
            left = new Tree(mantainputstream);
        else
        if(j != 0)
            left = (Tree)mantainputstream.getObjectFromCycleCheck(j);
    }

    public static final int PAYLOAD = 16;
    Tree left;
    Tree right;
    int i;
    int i1;
    int i2;
    int i3;
}

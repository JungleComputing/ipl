// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Tree.java

import java.io.Serializable;

public final class Tree implements Serializable {

    public Tree(int j)
    {
	int k = j / 2;
	if(k > 0)
	    left = new Tree(k);
	if(j - k - 1 > 0)
	    right = new Tree(j - k - 1);
    }

    public final void generated_WriteObject(int [] arr, int index) throws java.io.IOException
    {
	if ((index + 1) > 1024) {
	    index = 0;
	}
	arr[index++] = i3;

	if ((index + 1) > 1024) {
	    index = 0;
	}
	arr[index++] = i2;

	if ((index + 1) > 1024) {
	    index = 0;
	}

	arr[index++] = i1;

	if ((index + 1) > 1024) {
	    index = 0;
	}
	arr[index++] = i;

	//        mantaoutputstream.writeInt(i3);
	//        mantaoutputstream.writeInt(i2);
	//        mantaoutputstream.writeInt(i1);
	//        mantaoutputstream.writeInt(i);
	//        int j = mantaoutputstream.writeKnownObjectHeader(right);
	if(right != null) right.generated_WriteObject(arr, index);
	//        j = mantaoutputstream.writeKnownObjectHeader(left);
	if(left != null) left.generated_WriteObject(arr, index);
    }

    public static final int PAYLOAD = 16;
    Tree left;
    Tree right;
    int i;
    int i1;
    int i2;
    int i3;
}

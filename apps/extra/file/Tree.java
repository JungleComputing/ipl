// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Tree.java

import java.io.Serializable;

public final class Tree
    implements Serializable
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

    public static final int PAYLOAD = 16;
    Tree left;
    Tree right;
    int i;
    int i1;
    int i2;
    int i3;
}

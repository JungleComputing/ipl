// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   IbisIdentifier.java

package ibis.ipl.impl.messagePassing;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import java.io.Serializable;

final class IbisIdentifier
    implements ibis.ipl.IbisIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(ibis.ipl.impl.messagePassing.IbisIdentifier other)
    {
        return cpu == other.cpu;
    }

    public final boolean equals(java.lang.Object o)
    {
        if(o == this)
            return true;
        if(o instanceof ibis.ipl.impl.messagePassing.IbisIdentifier)
        {
            ibis.ipl.impl.messagePassing.IbisIdentifier other = (ibis.ipl.impl.messagePassing.IbisIdentifier)o;
            return cpu == other.cpu;
        } else
        {
            return false;
        }
    }

    public final java.lang.String toString()
    {
        return "(IbisIdent: name = " + name + ")";
    }

    public final java.lang.String name()
    {
        return name;
    }

    public final int hashCode()
    {
        return name.hashCode();
    }

    IbisIdentifier(java.lang.String name, int cpu)
    {
        this.name = name;
        this.cpu = cpu;
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream mantaoutputstream)
        throws ibis.ipl.IbisIOException
    {
        mantaoutputstream.writeInt(cpu);
        mantaoutputstream.writeUTF(name);
    }

    public IbisIdentifier(ibis.io.MantaInputStream mantainputstream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        cpu = mantainputstream.readInt();
        name = mantainputstream.readUTF();
    }

    java.lang.String name;
    int cpu;
}

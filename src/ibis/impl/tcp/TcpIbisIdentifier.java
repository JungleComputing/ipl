// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   TcpIbisIdentifier.java

package ibis.ipl.impl.tcp;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.generic.IbisIdentifierTable;
import java.io.Serializable;
import java.net.InetAddress;

// Referenced classes of package ibis.ipl.impl.tcp:
//            TcpIbis

public final class TcpIbisIdentifier
    implements ibis.ipl.IbisIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(java.lang.Object o)
    {
        if(o == this)
            return true;
        if(o instanceof ibis.ipl.impl.tcp.TcpIbisIdentifier)
        {
            ibis.ipl.impl.tcp.TcpIbisIdentifier other = (ibis.ipl.impl.tcp.TcpIbisIdentifier)o;
            return equals(other);
        } else
        {
            return false;
        }
    }

    public final boolean equals(ibis.ipl.impl.tcp.TcpIbisIdentifier other)
    {
        if(other == this)
            return true;
        else
            return address.equals(other.address) && name.equals(other.name);
    }

    public final java.lang.String toString()
    {
        return "(TcpId: " + name + " on [" + address.getHostName() + ", " + address.getHostAddress() + "])";
    }

    public final java.lang.String name()
    {
        return name;
    }

    public final int hashCode()
    {
        return name.hashCode();
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream stream)
        throws ibis.ipl.IbisIOException
    {
        int handle = ibis.ipl.impl.tcp.TcpIbis.globalIbis.identTable.getHandle(stream, this);
        stream.writeInt(handle);
        if(handle < 0)
        {
            stream.writeObject(address);
            stream.writeUTF(name);
        }
    }

    public TcpIbisIdentifier()
    {
    }

    public TcpIbisIdentifier(ibis.io.MantaInputStream stream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        stream.addObjectToCycleCheck(this);
        int handle = stream.readInt();
        if(handle < 0)
        {
            address = (java.net.InetAddress)stream.readObject();
            name = stream.readUTF();
            ibis.ipl.impl.tcp.TcpIbis.globalIbis.identTable.addIbis(stream, -handle, this);
        } else
        {
            ibis.ipl.impl.tcp.TcpIbisIdentifier ident = (ibis.ipl.impl.tcp.TcpIbisIdentifier)ibis.ipl.impl.tcp.TcpIbis.globalIbis.identTable.getIbis(stream, handle);
            address = ident.address;
            name = ident.name;
        }
    }

    public static final int serialversionID = 1;
    java.net.InetAddress address;
    java.lang.String name;
}

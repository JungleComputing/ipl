// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   ReceivePortIdentifier.java

package ibis.ipl.impl.messagePassing;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.IbisIdentifier;
import java.io.Serializable;

// Referenced classes of package ibis.ipl.impl.messagePassing:
//            Ibis

final class ReceivePortIdentifier
    implements ibis.ipl.ReceivePortIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(ibis.ipl.ReceivePortIdentifier other)
    {
        if(other == this)
            return true;
        if(!(other instanceof ibis.ipl.impl.messagePassing.ReceivePortIdentifier))
            return false;
        if(other instanceof ibis.ipl.impl.messagePassing.ReceivePortIdentifier)
        {
            ibis.ipl.impl.messagePassing.ReceivePortIdentifier temp = (ibis.ipl.impl.messagePassing.ReceivePortIdentifier)other;
            return cpu == temp.cpu && port == temp.port;
        } else
        {
            return false;
        }
    }

    public final java.lang.String name()
    {
        return name;
    }

    public final java.lang.String type()
    {
        return type;
    }

    public final ibis.ipl.IbisIdentifier ibis()
    {
        return ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier();
    }

    public final java.lang.String toString()
    {
        return "(RecPortIdent: name \"" + name + "\" type \"" + type + "\" cpu " + cpu + " port " + port + ")";
    }

    ReceivePortIdentifier(java.lang.String name, java.lang.String type, int cpu, int port)
    {
        this.name = name;
        this.type = type;
        this.cpu = cpu;
        this.port = port;
    }

    ReceivePortIdentifier(java.lang.String name, java.lang.String type)
    {
        ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
        port = ibis.ipl.impl.messagePassing.Ibis.myIbis.receivePort++;
        ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
        cpu = ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu;
        this.name = name;
        this.type = type;
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream mantaoutputstream)
        throws ibis.ipl.IbisIOException
    {
        mantaoutputstream.writeInt(port);
        mantaoutputstream.writeInt(cpu);
        mantaoutputstream.writeUTF(type);
        mantaoutputstream.writeUTF(name);
    }

    public ReceivePortIdentifier(ibis.io.MantaInputStream mantainputstream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        port = mantainputstream.readInt();
        cpu = mantainputstream.readInt();
        type = mantainputstream.readUTF();
        name = mantainputstream.readUTF();
    }

    java.lang.String name;
    java.lang.String type;
    int cpu;
    int port;
}

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   SendPortIdentifier.java

package ibis.ipl.impl.messagePassing;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.IbisIdentifier;
import java.io.Serializable;

// Referenced classes of package ibis.ipl.impl.messagePassing:
//            Ibis, IbisIdentifier

final class SendPortIdentifier
    implements ibis.ipl.SendPortIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(ibis.ipl.SendPortIdentifier other)
    {
        if(other == this)
            return true;
        if(other instanceof ibis.ipl.impl.messagePassing.SendPortIdentifier)
        {
            ibis.ipl.impl.messagePassing.SendPortIdentifier o = (ibis.ipl.impl.messagePassing.SendPortIdentifier)other;
            return cpu == o.cpu && port == o.port;
        } else
        {
            return false;
        }
    }

    public final java.lang.String name()
    {
        if(name != null)
            return name;
        else
            return "anonymous";
    }

    public final java.lang.String type()
    {
        return type;
    }

    public final ibis.ipl.IbisIdentifier ibis()
    {
        return ibisId;
    }

    public final java.lang.String toString()
    {
        return "(SendPortIdent: name \"" + name + "\" type \"" + type + "\" cpu " + cpu + " port " + port + ")";
    }

    SendPortIdentifier(java.lang.String name, java.lang.String type)
    {
        ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
        port = ibis.ipl.impl.messagePassing.Ibis.myIbis.sendPort++;
        ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
        this.name = name;
        this.type = type;
        ibisId = (ibis.ipl.impl.messagePassing.IbisIdentifier)ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier();
        cpu = ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu;
    }

    SendPortIdentifier(java.lang.String name, java.lang.String type, java.lang.String ibisId, int cpu, int port)
    {
        this.name = name;
        this.type = type;
        this.ibisId = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupIbis(ibisId);
        this.cpu = cpu;
        this.port = port;
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream mantaoutputstream)
        throws ibis.ipl.IbisIOException
    {
        mantaoutputstream.writeInt(port);
        mantaoutputstream.writeInt(cpu);
        mantaoutputstream.writeUTF(type);
        mantaoutputstream.writeUTF(name);
        int i = mantaoutputstream.writeKnownObjectHeader(ibisId);
        if(i == 1)
            ibisId.generated_WriteObject(mantaoutputstream);
    }

    public SendPortIdentifier(ibis.io.MantaInputStream mantainputstream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        port = mantainputstream.readInt();
        cpu = mantainputstream.readInt();
        type = mantainputstream.readUTF();
        name = mantainputstream.readUTF();
        int i = mantainputstream.readKnownTypeHeader();
        if(i == -1)
            ibisId = new ibis.ipl.impl.messagePassing.IbisIdentifier(mantainputstream);
        else
        if(i != 0)
            ibisId = (ibis.ipl.impl.messagePassing.IbisIdentifier)mantainputstream.getObjectFromCycleCheck(i);
    }

    java.lang.String name;
    java.lang.String type;
    int cpu;
    int port;
    ibis.ipl.impl.messagePassing.IbisIdentifier ibisId;
}

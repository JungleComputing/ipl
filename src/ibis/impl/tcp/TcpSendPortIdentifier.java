// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   TcpSendPortIdentifier.java

package ibis.ipl.impl.tcp;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;
import java.io.Serializable;

// Referenced classes of package ibis.ipl.impl.tcp:
//            TcpIbisIdentifier

public final class TcpSendPortIdentifier
    implements ibis.ipl.SendPortIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(ibis.ipl.impl.tcp.TcpSendPortIdentifier tcpsendportidentifier)
    {
        if(tcpsendportidentifier == null)
            return false;
        else
            return type.equals(tcpsendportidentifier.type) && ibis.equals(tcpsendportidentifier.ibis) && name.equals(tcpsendportidentifier.name);
    }

    public final boolean equals(ibis.ipl.SendPortIdentifier sendportidentifier)
    {
        if(sendportidentifier instanceof ibis.ipl.impl.tcp.TcpSendPortIdentifier)
            return equals((ibis.ipl.impl.tcp.TcpSendPortIdentifier)sendportidentifier);
        else
            return false;
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
        return ibis;
    }

    public final java.lang.String toString()
    {
        return "(TcpSendPortIdent: name = " + (name == null ? "anonymous" : name) + ", type = " + type + ", ibis = " + ibis + ")";
    }

    TcpSendPortIdentifier(java.lang.String s, java.lang.String s1, ibis.ipl.impl.tcp.TcpIbisIdentifier tcpibisidentifier)
    {
        name = s;
        type = s1;
        ibis = tcpibisidentifier;
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream mantaoutputstream)
        throws ibis.ipl.IbisIOException
    {
        mantaoutputstream.writeUTF(name);
        mantaoutputstream.writeUTF(type);
        int i = mantaoutputstream.writeKnownObjectHeader(ibis);
        if(i == 1)
            ibis.generated_WriteObject(mantaoutputstream);
    }

    public TcpSendPortIdentifier(ibis.io.MantaInputStream mantainputstream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        name = mantainputstream.readUTF();
        type = mantainputstream.readUTF();
        int i = mantainputstream.readKnownTypeHeader();
        if(i == -1)
            ibis = new TcpIbisIdentifier(mantainputstream);
        else
        if(i != 0)
            ibis = (ibis.ipl.impl.tcp.TcpIbisIdentifier)mantainputstream.getObjectFromCycleCheck(i);
    }

    java.lang.String type;
    java.lang.String name;
    ibis.ipl.impl.tcp.TcpIbisIdentifier ibis;
}

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: fullnames 
// Source File Name:   TcpReceivePortIdentifier.java

package ibis.ipl.impl.tcp;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import java.io.Serializable;

// Referenced classes of package ibis.ipl.impl.tcp:
//            TcpIbisIdentifier

public final class TcpReceivePortIdentifier
    implements ibis.ipl.ReceivePortIdentifier, java.io.Serializable, ibis.io.Serializable
{

    public final boolean equals(ibis.ipl.impl.tcp.TcpReceivePortIdentifier tcpreceiveportidentifier)
    {
        if(tcpreceiveportidentifier == null)
            return false;
        else
            return type.equals(tcpreceiveportidentifier.type) && ibis.equals(tcpreceiveportidentifier.ibis) && name.equals(tcpreceiveportidentifier.name);
    }

    public final boolean equals(ibis.ipl.ReceivePortIdentifier receiveportidentifier)
    {
        if(receiveportidentifier instanceof ibis.ipl.impl.tcp.TcpReceivePortIdentifier)
            return equals((ibis.ipl.impl.tcp.TcpReceivePortIdentifier)receiveportidentifier);
        else
            return false;
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
        return ibis;
    }

    public final java.lang.String toString()
    {
        return "(TcpRecPortIdent: name = " + name + ", type = " + type + ", ibis = " + ibis + ")";
    }

    TcpReceivePortIdentifier(java.lang.String s, java.lang.String s1, ibis.ipl.impl.tcp.TcpIbisIdentifier tcpibisidentifier)
    {
        name = s;
        type = s1;
        ibis = tcpibisidentifier;
    }

    public final void generated_WriteObject(ibis.io.MantaOutputStream mantaoutputstream)
        throws ibis.ipl.IbisIOException
    {
        mantaoutputstream.writeUTF(type);
        mantaoutputstream.writeUTF(name);
        int i = mantaoutputstream.writeKnownObjectHeader(ibis);
        if(i == 1)
            ibis.generated_WriteObject(mantaoutputstream);
    }

    public TcpReceivePortIdentifier(ibis.io.MantaInputStream mantainputstream)
        throws ibis.ipl.IbisIOException, java.lang.ClassNotFoundException
    {
        mantainputstream.addObjectToCycleCheck(this);
        type = mantainputstream.readUTF();
        name = mantainputstream.readUTF();
        int i = mantainputstream.readKnownTypeHeader();
        if(i == -1)
            ibis = new TcpIbisIdentifier(mantainputstream);
        else
        if(i != 0)
            ibis = (ibis.ipl.impl.tcp.TcpIbisIdentifier)mantainputstream.getObjectFromCycleCheck(i);
    }

    java.lang.String name;
    java.lang.String type;
    ibis.ipl.impl.tcp.TcpIbisIdentifier ibis;
}

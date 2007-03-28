package ibis.impl.stacking.dummy;

import ibis.impl.Ibis;
import ibis.impl.tcp.TcpIbis;
import ibis.ipl.CapabilitySet;
import ibis.impl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;

public class StackingIbis extends Ibis {
    Ibis base;
    HashMap<IbisIdentifier,IbisIdentifier> toBase = new HashMap<IbisIdentifier, IbisIdentifier>();
    HashMap<IbisIdentifier,IbisIdentifier> fromBase = new HashMap<IbisIdentifier, IbisIdentifier>();

    public StackingIbis(RegistryEventHandler registryHandler,
            CapabilitySet caps, Properties tp) {
        super(registryHandler, caps, tp, null);
    }

    public void printStatistics(PrintStream out) {
        super.printStatistics(out);
        base.printStatistics(out);
    }

    public void joined(IbisIdentifier[] joins) {
        super.joined(joins);
        for (int i = 0; i < joins.length; i++) {
            IbisIdentifier idBase = null;
            IbisIdentifier id = joins[i];
            try {
                idBase = new IbisIdentifier(id.getImplementationData());
            } catch(IOException e) {
                throw new Error("Internal error", e);
            }
            toBase.put(id, idBase);
            fromBase.put(idBase, id);
        }
    }
    
    public void left(IbisIdentifier[] leavers) {
        super.left(leavers);
        for (int i = 0; i < leavers.length; i++) {
            IbisIdentifier idBase = null;
            IbisIdentifier id = leavers[i];
            try {
                idBase = new IbisIdentifier(id.getImplementationData());
            } catch(IOException e) {
                throw new Error("Internal error", e);
            }
            toBase.remove(id);
            fromBase.remove(idBase);
        }
    }
    
    public void died(IbisIdentifier[] leavers) {
        super.died(leavers);
        for (int i = 0; i < leavers.length; i++) {
            IbisIdentifier idBase = null;
            IbisIdentifier id = leavers[i];
            try {
                idBase = new IbisIdentifier(id.getImplementationData());
            } catch(IOException e) {
                throw new Error("Internal error", e);
            }
            toBase.remove(id);
            fromBase.remove(idBase);
        }
    }
    
    @Override
    protected byte[] getData() throws IOException {
        // For now:
        Properties p = new Properties(properties);
        p.setProperty("ibis.registry.impl", "ibis.impl.registry.NullRegistry");
        base = new TcpIbis(null, capabilities, properties);
        return base.ident.toBytes();
    }
    
    ReceivePortIdentifier toBase(ReceivePortIdentifier id) {
        String name = id.name();
        IbisIdentifier ibisId = (IbisIdentifier) id.ibis();
        IbisIdentifier newId = toBase.get(ibisId);
        return new ibis.impl.ReceivePortIdentifier(name, newId);
    }
    
    ReceivePortIdentifier[] toBase(ReceivePortIdentifier[] ids) {
        ReceivePortIdentifier[] result = new ReceivePortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = toBase(ids[i]);
        }
        return result;
    }
    
    ReceivePortIdentifier fromBase(ReceivePortIdentifier id) {
        String name = id.name();
        IbisIdentifier ibisId = (IbisIdentifier) id.ibis();
        IbisIdentifier newId = fromBase.get(ibisId);
        return new ibis.impl.ReceivePortIdentifier(name, newId);
    }
    
    ReceivePortIdentifier[] fromBase(ReceivePortIdentifier[] ids) {
        ReceivePortIdentifier[] result = new ReceivePortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = fromBase(ids[i]);
        }
        return result;
    }
       
    SendPortIdentifier toBase(SendPortIdentifier id) {
        String name = id.name();
        IbisIdentifier ibisId = (IbisIdentifier) id.ibis();
        IbisIdentifier newId = toBase.get(ibisId);
        return new ibis.impl.SendPortIdentifier(name, newId);
    }
    
    SendPortIdentifier[] toBase(SendPortIdentifier[] ids) {
        SendPortIdentifier[] result = new SendPortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = toBase(ids[i]);
        }
        return result;
    }
    
    SendPortIdentifier fromBase(SendPortIdentifier id) {
        String name = id.name();
        IbisIdentifier ibisId = (IbisIdentifier) id.ibis();
        IbisIdentifier newId = fromBase.get(ibisId);
        return new ibis.impl.SendPortIdentifier(name, newId);
    }
    
    SendPortIdentifier[] fromBase(SendPortIdentifier[] ids) {
        SendPortIdentifier[] result = new SendPortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = fromBase(ids[i]);
        }
        return result;
    }
    
    @Override
    protected PortType newPortType(CapabilitySet p, Properties attrib) {
         return new StackingPortType(this, p, attrib);
    }

    @Override
    protected void quit() {
        base.end();
        
    }

}

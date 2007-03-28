package ibis.impl.stacking.dummy;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.tcp.TcpIbis;
import ibis.ipl.CapabilitySet;
import ibis.ipl.PortType;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;

public class StackingIbis extends Ibis {
    private static CapabilitySet cp
            = new CapabilitySet(PredefinedCapabilities.RESIZE_DOWNCALLS);
    Ibis base;
    private HashMap<IbisIdentifier,IbisIdentifier> toBase
            = new HashMap<IbisIdentifier, IbisIdentifier>();
    private HashMap<IbisIdentifier,IbisIdentifier> fromBase
            = new HashMap<IbisIdentifier, IbisIdentifier>();

    public StackingIbis(RegistryEventHandler registryHandler,
            CapabilitySet caps, Properties tp) {
        super(registryHandler, caps.uniteWith(cp), tp, null);
    }

    public void printStatistics(PrintStream out) {
        super.printStatistics(out);
        base.printStatistics(out);
    }

    public void joined(IbisIdentifier[] joins) {
        System.out.println("joined called");
        super.joined(joins);
        synchronized(this) {
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
            notifyAll();
        }
    }
    
    public void left(IbisIdentifier[] leavers) {
        super.left(leavers);
        synchronized(this) {
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
    }
    
    public void died(IbisIdentifier[] leavers) {
        super.died(leavers);
        synchronized(this) {
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
    }
    
    @Override
    protected byte[] getData() throws IOException {
        // For now:
        Properties p = new Properties(properties);
        CapabilitySet baseCp = capabilities.subtract(
                new CapabilitySet(PredefinedCapabilities.RESIZE_DOWNCALLS,
                    PredefinedCapabilities.RESIZE_UPCALLS));
        p.setProperty("ibis.registry.impl", "ibis.impl.registry.NullRegistry");
        base = new TcpIbis(null, baseCp, p);
        return base.ident.toBytes();
    }

    synchronized IbisIdentifier toBase(ibis.ipl.IbisIdentifier id) {
        IbisIdentifier newId = toBase.get(id);
        while (newId == null) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
            newId = toBase.get(id);
        }
        return newId;
    }
    
    synchronized IbisIdentifier fromBase(ibis.ipl.IbisIdentifier id) {
        IbisIdentifier newId = fromBase.get(id);
        while (newId == null) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
            newId = fromBase.get(id);
        }
        return newId;
    }
    
    ReceivePortIdentifier toBase(ReceivePortIdentifier id) {
        return new ibis.impl.ReceivePortIdentifier(id.name(),
                toBase(id.ibis()));
    }
    
    ReceivePortIdentifier[] toBase(ReceivePortIdentifier[] ids) {
        ReceivePortIdentifier[] result = new ReceivePortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = toBase(ids[i]);
        }
        return result;
    }
    
    ReceivePortIdentifier fromBase(ReceivePortIdentifier id) {
        return new ibis.impl.ReceivePortIdentifier(id.name(),
                fromBase(id.ibis()));
    }
    
    ReceivePortIdentifier[] fromBase(ReceivePortIdentifier[] ids) {
        ReceivePortIdentifier[] result = new ReceivePortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = fromBase(ids[i]);
        }
        return result;
    }
       
    SendPortIdentifier toBase(SendPortIdentifier id) {
        return new ibis.impl.SendPortIdentifier(id.name(), toBase(id.ibis()));
    }
    
    SendPortIdentifier[] toBase(SendPortIdentifier[] ids) {
        SendPortIdentifier[] result = new SendPortIdentifier[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = toBase(ids[i]);
        }
        return result;
    }
    
    SendPortIdentifier fromBase(SendPortIdentifier id) {
        return new ibis.impl.SendPortIdentifier(id.name(), fromBase(id.ibis()));
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

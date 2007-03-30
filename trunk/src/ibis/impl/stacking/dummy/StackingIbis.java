package ibis.impl.stacking.dummy;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.tcp.TcpIbis;
import ibis.ipl.CapabilitySet;
import ibis.ipl.PortType;
import ibis.ipl.PredefinedCapabilities;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
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

    public void joined(ibis.ipl.IbisIdentifier id) {
        System.out.println("joined called");
        super.joined(id);
        IbisIdentifier i = (IbisIdentifier) id;
        synchronized(this) {
            IbisIdentifier idBase = null;
            try {
                idBase = new IbisIdentifier(i.getImplementationData());
            } catch(IOException e) {
                throw new Error("Internal error", e);
            }
            toBase.put(i, idBase);
            fromBase.put(idBase, i);
            notifyAll();
        }
    }
    
    public void left(ibis.ipl.IbisIdentifier id) {
        super.left(id);
        synchronized(this) {
            IbisIdentifier idBase = null;
            try {
                idBase = new IbisIdentifier(((IbisIdentifier)id).getImplementationData());
            } catch(IOException e) {
                throw new Error("Internal error", e);
            }
            toBase.remove(id);
            fromBase.remove(idBase);
        }
    }
    
    public void died(ibis.ipl.IbisIdentifier id) {
        super.died(id);
        synchronized(this) {
            IbisIdentifier idBase = null;
            try {
                idBase = new IbisIdentifier(((IbisIdentifier)id).getImplementationData());
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
        CapabilitySet baseCp = capabilities.subtract(
                new CapabilitySet(PredefinedCapabilities.RESIZE_DOWNCALLS,
                    PredefinedCapabilities.WORLDMODEL_CLOSED,
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

    public Map<String, Object> dynamicProperties() {
        return base.dynamicProperties();
    }

    public Object getDynamicProperty(String key) {
        return base.getDynamicProperty(key);
    }

    public void setDynamicProperties(Map<String, Object> properties) {
        base.setDynamicProperties(properties);      
    }

    public void setDynamicProperty(String key, Object val) {
        base.setDynamicProperty(key, val);
    }
}

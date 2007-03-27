package ibis.impl.stacking.generic;

import ibis.impl.Ibis;
import ibis.impl.tcp.TcpIbis;
import ibis.ipl.CapabilitySet;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

public class StackingIbis extends Ibis {
    Ibis ibis;

    public StackingIbis(RegistryEventHandler registryHandler,
            CapabilitySet caps, Properties tp) throws Throwable {
        super(registryHandler, caps, tp, null);
    }

    public void printStatistics(PrintStream out) {
        super.printStatistics(out);
        ibis.printStatistics(out);
    }

    @Override
    protected byte[] getData() throws IOException {
        // For now:
        Properties p = new Properties(properties);
        p.setProperty("ibis.registry.impl", "ibis.impl.registry.NullRegistry");
        ibis = new TcpIbis(null, capabilities, properties);
        return ibis.ident.toBytes();
    }

    @Override
    protected PortType newPortType(CapabilitySet p, Properties attrib) {
         return new StackingPortType(this, p, attrib);
    }

    @Override
    protected void quit() {
        ibis.end();
        
    }

}

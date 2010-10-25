package ibis.ipl.util.rpc;

import java.io.IOException;
import java.util.Date;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;

public class Example {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    private final Ibis myIbis;

    public interface ExampleInterface {
        public String function();
    }

    public class ExampleClass implements ExampleInterface {
        public String function() {
            return new Date().toString();
        }
    }

    /**
     * Constructor. Actually does all the work too :)
     */
    private Example() throws Exception {
        // Create an ibis instance.
        // Notice createIbis uses varargs for its parameters.
        myIbis = IbisFactory.createIbis(ibisCapabilities, null,
                RPC.rpcPortTypes);

        // Elect a server
        IbisIdentifier server = myIbis.registry().elect("Server");

        // If I am the server, run server, else run client.
        if (server.equals(myIbis.identifier())) {
            server();
        } else {
            client(server);
        }

        // End ibis.
        myIbis.end();
    }

    private void client(IbisIdentifier server) throws Exception {
        
        ExampleInterface interfaceObject = (ExampleInterface) RPC.createProxy(ExampleInterface.class, server, "my great object", myIbis);
        
        System.err.println(interfaceObject.function());

    }

    private void server() throws Exception {

        ExampleClass object = new ExampleClass();

        RemoteObject remoteObject = new RemoteObject(myIbis, "my great object",
                object, ExampleInterface.class);
        
        Thread.sleep(100000);
        
        remoteObject.unexport();
    }

    public static void main(String args[]) {
        try {
            new Example();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}

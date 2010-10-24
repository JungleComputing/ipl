package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.examples.ClientServer;

import java.io.IOException;
import java.util.Date;

public class Example {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    private final Ibis myIbis;

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



    private void client(IbisIdentifier server) {
		// TODO Auto-generated method stub
		
	}



	private void server() {
		// TODO Auto-generated method stub
		
	}



	public static void main(String args[]) {
        try {
            new Example();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}

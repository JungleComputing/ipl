/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.util.rpc;

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
		
		// Converts epoch time to date string.
		public String millisToString(long millis) throws RemoteException, Exception;
	}

	public class ExampleClass implements ExampleInterface {
		public String millisToString(long millis) throws RemoteException, Exception {
			return "rpc example result = " + new Date(millis).toString();
		}
	}

	/**
	 * Constructor. Actually does all the work too :)
	 */
	private Example() throws Exception {
		// Create an ibis instance.
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

	private void server() throws Exception {

		//create object we want to make remotely accessible
		ExampleClass object = new ExampleClass();

		//make object remotely accessible
		RemoteObject<ExampleInterface> remoteObject = RPC.exportObject(
				ExampleInterface.class, object, "my great object", myIbis);

		//wait for a bit
		Thread.sleep(100000);

		//cleanup, object no longer remotely accessible
		remoteObject.unexport();
	}
	
	private void client(IbisIdentifier server) throws Exception {

		//create proxy to remote object
		ExampleInterface interfaceObject = RPC.createProxy(
				ExampleInterface.class, server, "my great object", myIbis);

		//call remote object, print result
		System.err.println(interfaceObject.millisToString(System.currentTimeMillis()));

	}


	public static void main(String args[]) {
		try {
			new Example();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}

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
package ibis.ipl.support.management;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.server.Server;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class Example {

    private static class Shutdown extends Thread {
        private final Server server;

        Shutdown(Server server) {
            this.server = server;
        }

        public void run() {
            server.end(-1);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] arguments) {

        // start a server
        Server server = null;
        try {
            server = new Server(new Properties());
        } catch (Throwable t) {
            System.err.println("Could not start Server: " + t);
            System.exit(1);
        }

        // print server description
        System.err.println(server.toString());

        // register shutdown hook
        try {
            Runtime.getRuntime().addShutdownHook(new Shutdown(server));
        } catch (Exception e) {
            System.err.println("warning: could not registry shutdown hook");
        }

        AttributeDescription load = new AttributeDescription(
                "java.lang:type=OperatingSystem", "SystemLoadAverage");

        AttributeDescription cpu = new AttributeDescription(
                "java.lang:type=OperatingSystem", "ProcessCpuTime");

        AttributeDescription vivaldi = new AttributeDescription("ibis",
                "vivaldi");

        AttributeDescription connections = new AttributeDescription("ibis",
                "connections");
        
        AttributeDescription sentBytesPerIbis = new AttributeDescription("ibis",
                "sentBytesPerIbis");
        
        AttributeDescription receivedBytesPerIbis = new AttributeDescription("ibis",
                "receivedBytesPerIbis");
        
        AttributeDescription wonElections = new AttributeDescription("ibis",
                "wonElections");
        
        AttributeDescription senderConnectionTypes = new AttributeDescription("ibis",
                "senderConnectionTypes");
        
        AttributeDescription receiverConnectionTypes = new AttributeDescription("ibis",
                "receiverConnectionTypes");
        
        
        while (true) {

            // get list of ibises in the pool named "test"
            IbisIdentifier[] ibises = server.getRegistryService().getMembers(
                    "test");

            // for each ibis, print these attributes
            if (ibises != null) {
                for (IbisIdentifier ibis : ibises) {
                    try {
                        System.err
                                .println(ibis
                                        + " [load, total cpu time, vivaldi coordinates] = "
                                        + Arrays.toString(server
                                                .getManagementService()
                                                .getAttributes(ibis, load, cpu,
                                                        vivaldi)));
                        System.err
                                .println(ibis
                                        + " connected to = "
                                        + Arrays
                                                .toString((IbisIdentifier[]) server
                                                        .getManagementService()
                                                        .getAttributes(ibis,
                                                                connections)[0]));
                        
                        Map<IbisIdentifier, Long> sent = (Map<IbisIdentifier, Long>)
                                (server.getManagementService().getAttributes(ibis, sentBytesPerIbis)[0]);
                        
                        if (sent != null) {
                            for (Entry<IbisIdentifier, Long> e : sent.entrySet()) {
                                System.err.println(ibis + " wrote " + e.getValue() + " bytes to " + e.getKey());
                            }
                        }
                        
                        Map<IbisIdentifier, Long> received = (Map<IbisIdentifier, Long>)
                                (server.getManagementService().getAttributes(ibis, receivedBytesPerIbis)[0]);
                        
                        if (received != null) {
                            for (Entry<IbisIdentifier, Long> e : received.entrySet()) {
                                System.err.println(ibis + " read " + e.getValue() + " bytes from " + e.getKey());
                            }
                        }
                        
                        String[] won = (String[])
                            (server.getManagementService().getAttributes(ibis, wonElections)[0]);
                
                        if (won != null) {
                            for (String s : won) {
                                System.err.println(ibis + " won election " + s);
                            }
                        }
                        
                        Map<IbisIdentifier, Set<String>> senderConnections =
                            (Map<IbisIdentifier, Set<String>>) (server.getManagementService().getAttributes(ibis, 
                                    senderConnectionTypes))[0];
                        
                        if (senderConnections != null) {
                            System.err.println("senderConnections: " + senderConnections.toString());
                        }
                        
                        Map<IbisIdentifier, Set<String>> receiverConnections =
                            (Map<IbisIdentifier, Set<String>>) (server.getManagementService().getAttributes(ibis, 
                                    receiverConnectionTypes))[0];
                        
                        if (receiverConnections != null) {
                            System.err.println("receiverConnections: " + receiverConnections.toString());
                        }

                    } catch (Exception e) {
                        System.err.println("Could not get management info: ");
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }

        }
    }

}

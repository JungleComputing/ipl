package ibis.server.remote;

import java.util.Map;

public class RemoteTest {

    public static void main(String[] args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command().add("ipl-server");
        builder.command().add("--remote");

        Process process = builder.start();

        System.err.println("started server");

        RemoteClient client = new RemoteClient(process.getInputStream(), process.getOutputStream());

        new StreamForwarder(process.getErrorStream(), System.err);

        System.err.println("started client");

        System.err
                .println("server local address = " + client.getLocalAddress());

        client.addHubs("localhost:5332");
        
        client.addHubs("bla");

        client.addHubs("machine:5");

        client.addHubs("localhost:4345");

        client.addHubs("machine.domain.nl:543");

        String[] hubs = client.getHubs();

        for (String hub : hubs) {
            System.err.println("hub = " + hub);
        }

        String[] services = client.getServiceNames();

        for (String service : services) {
            System.err.println("service = " + service);
        }
        
        Map<String, String> statistics = client.getStats("registry");
        
        System.err.println("registry statistics:");
        for(Map.Entry<String, String> entry: statistics.entrySet()) {
            System.err.println(entry.getKey() + " = " + entry.getValue());
        }

        System.err.println("ending server");
        client.end(0);
        System.err.println("ended");

        process.destroy();
    }

}

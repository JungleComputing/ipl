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
package ibis.ipl.registry.central;

import java.util.LinkedHashMap;
import java.util.Map;

import ibis.util.TypedProperties;

public class RegistryProperties {

    public static final String PREFIX = "ibis.registry.central.";

    public static final String HEARTBEAT_INTERVAL = PREFIX + "heartbeat.interval";

    public static final String EXIT_ON_SERVER_FAILURE = PREFIX + "exit.on.server.failure";

    public static final String EVENT_PUSH_INTERVAL = PREFIX + "event.push.interval";

    public static final String GOSSIP = PREFIX + "gossip";

    public static final String GOSSIP_INTERVAL = PREFIX + "gossip.interval";

    public static final String ADAPT_GOSSIP_INTERVAL = PREFIX + "adapt.gossip.interval";

    public static final String TREE = PREFIX + "tree";

    public static final String PURGE_HISTORY = PREFIX + "purge.history";

    public static final String PEER_BOOTSTRAP = PREFIX + "peer.bootstrap";

    public static final String CLIENT_CONNECT_TIMEOUT = PREFIX + "client.connect.timeout";

    public static final String SERVER_CONNECT_TIMEOUT = PREFIX + "server.connect.timeout";

    public static final String STATISTICS = PREFIX + "statistics";

    public static final String STATISTICS_INTERVAL = PREFIX + "statistics.interval";

    // list of descriptions and defaults
    private static final String[][] propertiesList = new String[][] {

            { HEARTBEAT_INTERVAL, "120", "Int(seconds): how often is a member " + "of a pool expected to report it is still alive" },

            { EXIT_ON_SERVER_FAILURE, "false", "Boolean: if true, clients will terminate the JVM if the server becomes unreachable" },

            { EVENT_PUSH_INTERVAL, "30", "Int(seconds): how often does the server " + "send out events any member may have missed" },

            { GOSSIP, "false", "Boolean: do we gossip, or send events centrally" },

            { GOSSIP_INTERVAL, "1", "Int(seconds): how often do we gossip" },

            { ADAPT_GOSSIP_INTERVAL, "false", "Boolean: if true, the server gossips more often if there are " + "more nodes in a pool" },

            { TREE, "false", "Boolean: use a broadcast tree instead of " + "serial send or gossiping" },

            { PEER_BOOTSTRAP, "true", "Boolean: bootstrap with peers, not just with the server" },

            { PURGE_HISTORY, "false", "Boolean: purge history when not needed anymore" },

            { CLIENT_CONNECT_TIMEOUT, "300", "Int(seconds): how long do clients attempt to connect to the server and peers before giving up" },

            { SERVER_CONNECT_TIMEOUT, "10", "Int(seconds): how long does the server attempt to connect to a client before giving up" },

            { STATISTICS, "false", "Boolean: gather per-pool statistics at the server and save them to a file" },

            { STATISTICS_INTERVAL, "60", "Int(seconds): how often do we send statistics to the server" },

    };

    public static TypedProperties getHardcodedProperties() {
        TypedProperties properties = new TypedProperties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new LinkedHashMap<>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

}

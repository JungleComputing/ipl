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
package ibis.ipl.registry.gossip;

import java.util.LinkedHashMap;
import java.util.Map;

import ibis.util.TypedProperties;

public class RegistryProperties {

    public static final String PREFIX = "ibis.registry.gossip.";

    public static final String GOSSIP_INTERVAL = PREFIX + "gossip.interval";

    public static final String GOSSIP_COUNT = PREFIX + "gossip.count";

    public static final String BOOTSTRAP_LIST = PREFIX + "bootstrap.list";

    public static final String WITNESSES_REQUIRED = PREFIX + "witnesses.required";

    public static final String PEER_DEAD_TIMEOUT = PREFIX + "peer.dead.timeout";

    public static final String PING_INTERVAL = PREFIX + "ping.interval";

    public static final String PING_COUNT = PREFIX + "ping.count";

    public static final String ELECTION_TIMEOUT = PREFIX + "election.timeout";

    public static final String STATISTICS = PREFIX + "statistics";

    public static final String STATISTICS_INTERVAL = PREFIX + " statistics.interval";

    public static final String LEAVES_SEND = PREFIX + "leaves.send";

    public static final String PRINT_MEMBERS = PREFIX + "print.members";

    // list of descriptions and defaults
    private static final String[][] propertiesList = new String[][] { { GOSSIP_INTERVAL, "1", "How often do we gossip (in seconds)" },
            { GOSSIP_COUNT, "30000", "How many members do we transfer (maximum)" }, { BOOTSTRAP_LIST, null, "List of peers to bootstrap of off" },
            { WITNESSES_REQUIRED, "5", "Int: how many peers need to agree before a node is declared dead" },
            { PEER_DEAD_TIMEOUT, "120", "Number of seconds until a peer can be declared dead" },
            { PING_INTERVAL, "1", "How often do we try to reach a suspect member (in seconds)" },
            { PING_COUNT, "10", "How many suspect members do we ping in each ping round" },
            { ELECTION_TIMEOUT, "5", "Number of seconds until we return the value of an election" },
            { STATISTICS, "false", "Boolean: if true, statistics are kept and written to a file named statistics/POOL_NAME/ID" },
            { STATISTICS_INTERVAL, "60", "Int: how often statistics are written to disk (in seconds)" },

            { LEAVES_SEND, "10", "Int: how many nodes do we send a message to saying we are leaving" },
            { PRINT_MEMBERS, "false", "Boolean: if true, the list of members is printed periodically" },

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

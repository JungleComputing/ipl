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

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionSet implements Iterable<Election> {

    private static final Logger logger = LoggerFactory.getLogger(ElectionSet.class);

    private final TypedProperties properties;

    private final Registry registry;

    private final HashMap<String, Election> elections;

    public ElectionSet(TypedProperties properties, Registry registry) {
        this.properties = properties;
        this.registry = registry;

        elections = new HashMap<String, Election>();
    }

    public synchronized void writeGossipData(DataOutputStream out)
            throws IOException {
	if (logger.isDebugEnabled()) {
	    logger.debug("writing " + elections.size() + " elections");
	}
        out.writeInt(elections.size());

        for (Election election : elections.values()) {
            election.writeTo(out);
        }
    }

    public void readGossipData(DataInputStream in) throws IOException {
        int nrOfElections = in.readInt();

        if (nrOfElections < 0) {
            throw new IOException("negative election list value");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("reading " + nrOfElections + " elections");
        }

        for (int i = 0; i < nrOfElections; i++) {
            Election election = new Election(in);
            String name = election.getName();

            if (elections.containsKey(name)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("merging " + election + " with existing election" + elections.get(name));
                }
                elections.get(name).merge(election);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("received new election in gossip: " + election);
                }

                elections.put(name, election);
            }
        }
    }

    public synchronized IbisIdentifier[] getElectionResult(String electionName,
            long timeoutMillis) {
	if (logger.isDebugEnabled()) {
	    logger.debug("trying to get election result for: " + electionName
		    + ", waiting " + timeoutMillis);
	}

        if (timeoutMillis > 0) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        Election election = elections.get(electionName);

        if (logger.isDebugEnabled()) {
            logger.debug("result for " + electionName + " is " + election);
        }

        if (election == null) {
            return null;
        }

        return election.getCandidates();
    }
    
    public IbisIdentifier[] getElectionResult(String electionName) {
        return getElectionResult(
            electionName,
            properties.getIntProperty(RegistryProperties.ELECTION_TIMEOUT) * 1000);
    }

    public synchronized IbisIdentifier[] elect(String electionName,
            long timeoutMillis) {
	if (logger.isDebugEnabled()) {
	    logger.debug("electing result for: " + electionName + ", waiting "
		    + timeoutMillis);
	}
	    
        Election election = elections.get(electionName);

        if (election == null) {
            election = new Election(electionName);
            elections.put(electionName, election);
        }

        if (election.nrOfCandidates() == 0) {
            election.addCandidate(registry.getIbisIdentifier());
        }

        if (timeoutMillis > 0) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // re-fetch election (just in case it got removed while we were waiting)
        election = elections.get(electionName);

        if (election == null) {
            election = new Election(electionName);
            elections.put(electionName, election);
        }

        if (election.nrOfCandidates() == 0) {
            election.addCandidate(registry.getIbisIdentifier());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("result for " + electionName + " is " + election);
        }

        return election.getCandidates();
    }

    public IbisIdentifier[] elect(String electionName) {
        return elect(
            electionName,
            properties.getIntProperty(RegistryProperties.ELECTION_TIMEOUT) * 1000);
    }

    @Override
    public Iterator<Election> iterator() {
        return elections.values().iterator();
    }

}

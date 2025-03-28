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
package ibis.ipl.impl.multi;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.Registry;
import ibis.util.ThreadPool;

public class MultiRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(MultiRegistry.class);

    private final MultiIbis ibis;

    private final ManageableMapper ManageableMapper;

    private final HashMap<String, Registry> subRegistries;

    final Map<MultiIbisIdentifier, MultiIbisIdentifier> joined = Collections.synchronizedMap(new HashMap<MultiIbisIdentifier, MultiIbisIdentifier>());
    final Map<MultiIbisIdentifier, MultiIbisIdentifier> left = Collections.synchronizedMap(new HashMap<MultiIbisIdentifier, MultiIbisIdentifier>());
    final Map<MultiIbisIdentifier, MultiIbisIdentifier> died = Collections.synchronizedMap(new HashMap<MultiIbisIdentifier, MultiIbisIdentifier>());
    final Map<String, MultiIbisIdentifier> elected = Collections.synchronizedMap(new HashMap<String, MultiIbisIdentifier>());

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MultiRegistry(MultiIbis multiIbis) {
        this.ibis = multiIbis;
        subRegistries = new HashMap<>();
        for (String ibisName : ibis.subIbisMap.keySet()) {
            Ibis subIbis = ibis.subIbisMap.get(ibisName);
            if (logger.isDebugEnabled()) {
                logger.debug("Registry for: " + ibisName + " : " + subIbis.registry());
            }
            subRegistries.put(ibisName, subIbis.registry());
        }
        ManageableMapper = new ManageableMapper((Map) subRegistries);
    }

    @Override
    public void assumeDead(IbisIdentifier ibisIdentifier) throws IOException {
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            subRegistry.assumeDead(((MultiIbisIdentifier) ibisIdentifier).subIdForIbis(ibisName));
        }
    }

    @Override
    public IbisIdentifier[] diedIbises() {
        HashMap<IbisIdentifier, String> theDead = new HashMap<>();
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.diedIbises();
            for (IbisIdentifier id : ids) {
                try {
                    theDead.put(ibis.mapIdentifier(id, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this
                }
            }
        }
        return theDead.keySet().toArray(new IbisIdentifier[theDead.size()]);
    }

    @Override
    public void disableEvents() {
        for (Registry subRegistry : subRegistries.values()) {
            subRegistry.disableEvents();
        }
    }

    @Override
    public IbisIdentifier elect(String electionName) throws IOException {
        return elect(electionName, 0);
    }

    private static final class ElectionRunner implements Runnable {

        private final Registry subRegistry;
        private final String electionName;
        private final long timeoutMillis;
        private final List<IbisIdentifierWrapper> elected;
        private final String ibisName;

        public ElectionRunner(String ibisName, Registry subRegistry, List<IbisIdentifierWrapper> elected, String electionName, long timeoutMillis) {
            this.subRegistry = subRegistry;
            this.electionName = electionName;
            this.timeoutMillis = timeoutMillis;
            this.elected = elected;
            this.ibisName = ibisName;
        }

        @Override
        public void run() {
            IbisIdentifier winner = null;
            try {
                winner = subRegistry.elect(electionName, timeoutMillis);
                if (logger.isDebugEnabled()) {
                    logger.debug("SubRegistry: " + subRegistry + " elected: " + winner);
                }
            } catch (IOException e) {
                // Ignored
            }
            synchronized (elected) {
                if (winner != null) {
                    elected.add(new IbisIdentifierWrapper(ibisName, winner));
                }
                elected.notify();
            }
        }
    }

    @Override
    public IbisIdentifier elect(final String electionName, final long timeoutMillis) throws IOException {
        if (subRegistries.size() > 0) {
            // TODO: Need to kick off these elections in parallel
            // TODO This is dumb stupid election management that wont work a lot
            // of the time
            final List<IbisIdentifierWrapper> elected = new ArrayList<>();
            synchronized (elected) {
                for (String ibisName : subRegistries.keySet()) {
                    Registry subRegistry = subRegistries.get(ibisName);
                    ThreadPool.createNew(new ElectionRunner(ibisName, subRegistry, elected, electionName, timeoutMillis),
                            "Election: " + electionName);
                }
                while (elected.size() < subRegistries.size()) {
                    try {
                        // Wait for the timeout
                        if (logger.isDebugEnabled()) {
                            logger.debug("Waiting for election: " + electionName + " count: " + elected.size() + " of: " + subRegistries.size()
                                    + " for: " + timeoutMillis);
                        }
                        elected.wait(timeoutMillis);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Woke up election: " + electionName + " count:" + elected.size() + " of: " + subRegistries.size());
                        }
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
                if (!elected.isEmpty()) {
                    Collections.sort(elected);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Elected: " + elected.get(0));
                    }
                    return ibis.mapIdentifier(elected.get(0).id, elected.get(0).ibisName);
                } else {
                    throw new Error("No election results!");
                }
            }
        } else {
            throw new Error("No Subregistries to support elections.");
        }
    }

    @Override
    public void enableEvents() {
        for (Registry subRegistry : subRegistries.values()) {
            subRegistry.enableEvents();
        }
    }

    @Override
    public IbisIdentifier getElectionResult(String electionName) throws IOException {
        return getElectionResult(electionName, 0);
    }

    @Override
    public IbisIdentifier getElectionResult(String electionName, long timeoutMillis) throws IOException {
        IbisIdentifier results = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Getting Election Results for: " + electionName + " timeout: " + timeoutMillis);
        }
        timeoutMillis = timeoutMillis / subRegistries.size();
        // TODO This is dumb stupid election management that wont work a lot of
        // the time
        ArrayList<IbisIdentifier> elected = new ArrayList<>();
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            // TODO: This expands the timeout.
            IbisIdentifier winner = subRegistry.getElectionResult(electionName, timeoutMillis);
            if (winner != null) {
                elected.add(ibis.mapIdentifier(winner, ibisName));
            }
        }
        if (!elected.isEmpty()) {
            Collections.sort(elected);
            results = elected.get(0);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("ElectionResult : " + electionName + " : " + results);
        }
        return results;
    }

    @Override
    public int getPoolSize() {
        int poolSize = 0;
        for (Registry subRegistry : subRegistries.values()) {
            poolSize += subRegistry.getPoolSize();
        }
        return poolSize;
    }

    @Override
    public String getPoolName() {
        // FIXME:does this make sense? -Niels
        return ibis.identifier().poolName();
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        // TODO: Any way to support sequences?
        throw new IOException("Sequences not supported!");
    }

    @Override
    public IbisIdentifier[] joinedIbises() {
        HashMap<IbisIdentifier, String> theJoined = new HashMap<>();
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.joinedIbises();
            for (IbisIdentifier id : ids) {
                try {
                    theJoined.put(ibis.mapIdentifier(id, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this?
                }
            }
        }
        return theJoined.keySet().toArray(new IbisIdentifier[theJoined.size()]);
    }

    @Override
    public IbisIdentifier[] leftIbises() {
        HashMap<IbisIdentifier, String> theLeft = new HashMap<>();
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.leftIbises();
            for (IbisIdentifier id : ids) {
                try {
                    theLeft.put(ibis.mapIdentifier(id, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this?
                }
            }
        }
        return theLeft.keySet().toArray(new IbisIdentifier[theLeft.size()]);
    }

    @Override
    public void maybeDead(IbisIdentifier ibisIdentifier) throws IOException {
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            subRegistry.maybeDead(ibis.mapIdentifier(ibisIdentifier, ibisName));
        }
    }

    @Override
    public String[] receivedSignals() {
        HashMap<String, String> theSignals = new HashMap<>();
        for (Registry subRegistry : subRegistries.values()) {
            String[] signals = subRegistry.receivedSignals();
            for (String signal : signals) {
                theSignals.put(signal, signal);
            }
        }
        return theSignals.keySet().toArray(new String[theSignals.size()]);
    }

    @Override
    public void signal(String signal, IbisIdentifier... ibisIdentifiers) throws IOException {
        IbisIdentifier[] ids = new IbisIdentifier[ibisIdentifiers.length];
        for (String ibisName : subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            for (int i = 0; i < ids.length; i++) {
                ids[i] = ((MultiIbisIdentifier) ibisIdentifiers[i]).subIdForIbis(ibisName);
            }
            subRegistry.signal(signal, ids);
        }
    }

    @Override
    public boolean isClosed() {
        // FIXME: is this correct? - Niels
        for (Registry subRegistry : subRegistries.values()) {
            if (!subRegistry.isClosed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void waitUntilPoolClosed() {
        for (Registry subRegistry : subRegistries.values()) {
            subRegistry.waitUntilPoolClosed();
        }
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return ManageableMapper.getManagementProperty(key);
    }

    @Override
    public Map<String, String> managementProperties() {
        return ManageableMapper.managementProperties();
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        ManageableMapper.printManagementProperties(stream);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        ManageableMapper.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        ManageableMapper.setManagementProperty(key, value);
    }

    @Override
    public boolean hasTerminated() {
        // FIXME: implement termination for this registry
        throw new IbisConfigurationException("termination not supported by MultiRegistry");
    }

    @Override
    public void terminate() throws IOException {
        throw new IbisConfigurationException("termination not supported by MultiRegistry");
    }

    @Override
    public IbisIdentifier waitUntilTerminated() {
        throw new IbisConfigurationException("termination not supported by MultiRegistry");
    }

    @Override
    public void addTokens(String name, int count) throws IOException {
        throw new IOException("Tokens not supported in this version");
    }

    @Override
    public String getToken(String name) throws IOException {
        throw new IOException("Tokens not supported in this version");
    }
}

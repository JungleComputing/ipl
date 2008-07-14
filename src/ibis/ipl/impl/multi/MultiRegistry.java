package ibis.ipl.impl.multi;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.Registry;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class MultiRegistry implements Registry{

    private static final Logger logger = Logger.getLogger(MultiRegistry.class);

    private final MultiIbis ibis;

    private final ManageableMapper ManageableMapper;

    private final HashMap<String, Registry>subRegistries;

    @SuppressWarnings("unchecked")
    public MultiRegistry(MultiIbis multiIbis) {
        this.ibis = multiIbis;
        subRegistries = new HashMap<String, Registry>();
        for (String ibisName:ibis.subIbisMap.keySet()) {
            Ibis subIbis = ibis.subIbisMap.get(ibisName);
            if (logger.isDebugEnabled()) {
                logger.debug("Registry for: " + ibisName + " : "+ subIbis.registry());
            }
            subRegistries.put(ibisName, subIbis.registry());
        }
        ManageableMapper = new ManageableMapper((Map)subRegistries);
    }

    public void assumeDead(IbisIdentifier ibisIdentifier) throws IOException {
        for(String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            subRegistry.assumeDead(((MultiIbisIdentifier)ibisIdentifier).subIdForIbis(ibisName));
        }
    }

    public IbisIdentifier[] diedIbises() {
        HashMap<IbisIdentifier, String> theDead = new HashMap<IbisIdentifier, String>();
        for (String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.diedIbises();
            for(int i=0; i<ids.length; i++) {
                try {
                    theDead.put(ibis.mapIdentifier(ids[i], ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this
                }
            }
        }
        return theDead.keySet().toArray(new IbisIdentifier[theDead.size()]);
    }

    public void disableEvents() {
        for (Registry subRegistry:subRegistries.values()) {
            subRegistry.disableEvents();
        }
    }

    public IbisIdentifier elect(String electionName) throws IOException {
        return elect(electionName, 0);
    }

    private final class ElectionRunner  implements Runnable {

        private final Registry subRegistry;
        private final String electionName;
        private final long timeoutMillis;
        private final List<IbisIdentifierWrapper>elected;
        private final String ibisName;

        public ElectionRunner(String ibisName, Registry subRegistry, List<IbisIdentifierWrapper>elected, String electionName, long timeoutMillis) {
            this.subRegistry = subRegistry;
            this.electionName = electionName;
            this.timeoutMillis = timeoutMillis;
            this.elected = elected;
            this.ibisName = ibisName;
        }

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
            synchronized(elected) {
                if (winner != null) {
                    elected.add(new IbisIdentifierWrapper(ibisName, winner));
                }
                elected.notify();
            }
        }
    }

    public IbisIdentifier elect(final String electionName, final long timeoutMillis)
            throws IOException {
        if (subRegistries.size() > 0) {
            // TODO: Need to kick off these elections in parallel
            // TODO This is dumb stupid election management that wont work a lot of the time
            final List<IbisIdentifierWrapper>elected = new ArrayList<IbisIdentifierWrapper>();
            synchronized (elected) {
                for (String ibisName: subRegistries.keySet()) {
                    Registry subRegistry = subRegistries.get(ibisName);
                    ThreadPool.createNew(new ElectionRunner(ibisName, subRegistry, elected, electionName, timeoutMillis), "Election: " + electionName);
                }
                while (elected.size() < subRegistries.size()) {
                    try {
                        // Wait for the timeout
                        if (logger.isDebugEnabled()) {
                            logger.debug("Waiting for election: " + electionName + " count: " + elected.size()+ " of: " + subRegistries.size() + " for: " + timeoutMillis);
                        }
                        elected.wait(timeoutMillis);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Woke up election: " + electionName + " count:" + elected.size() + " of: " + subRegistries.size());
                        }
                    }
                    catch (InterruptedException e) {
                        // Ignored
                    }
                }
                if (!elected.isEmpty()) {
                    Collections.sort(elected);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Elected: " + ibis.mapIdentifier(elected.get(0).id, elected.get(0).ibisName));
                    }
                    return ibis.mapIdentifier(elected.get(0).id, elected.get(0).ibisName);
                }
                else {
                    throw new Error("No election results!");
                }
            }
        }
        else {
            throw new Error("No Subregistries to support elections.");
        }
    }

    public void enableEvents() {
        for (Registry subRegistry:subRegistries.values()) {
            subRegistry.enableEvents();
        }
    }

    public IbisIdentifier getElectionResult(String electionName)
            throws IOException {
        return getElectionResult(electionName, 0);
    }

    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) throws IOException {
        timeoutMillis = timeoutMillis / subRegistries.size();
        // TODO This is dumb stupid election management that wont work a lot of the time
        ArrayList<IbisIdentifier>elected = new ArrayList<IbisIdentifier>();
        for (Registry subRegistry: subRegistries.values()) {
            IbisIdentifier winner = subRegistry.getElectionResult(electionName, timeoutMillis);
            if (winner != null) {

            }
        }
        if (!elected.isEmpty()) {
            Collections.sort(elected);
            return elected.get(0);
        }
        return null;
    }

    public int getPoolSize() {
        int poolSize = 0;
        for (Registry subRegistry: subRegistries.values()) {
            poolSize += subRegistry.getPoolSize();
        }
        return poolSize;
    }

    public long getSequenceNumber(String name) throws IOException {
        // TODO: Any way to support sequences?
        throw new IOException("Sequences not supported!");
    }

    public IbisIdentifier[] joinedIbises() {
        HashMap<IbisIdentifier, String> theJoined = new HashMap<IbisIdentifier, String>();
        for (String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.joinedIbises();
            for(int i=0; i<ids.length; i++) {
                try {
                    theJoined.put(ibis.mapIdentifier(ids[i], ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this?
                }
            }
        }
        return theJoined.keySet().toArray(new IbisIdentifier[theJoined.size()]);
    }

    public IbisIdentifier[] leftIbises() {
        HashMap<IbisIdentifier, String> theLeft = new HashMap<IbisIdentifier, String>();
        for (String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            IbisIdentifier[] ids = subRegistry.leftIbises();
            for(int i=0; i<ids.length; i++) {
                try {
                    theLeft.put(ibis.mapIdentifier(ids[i], ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we be ignoring this?
                }
            }
        }
        return theLeft.keySet().toArray(new IbisIdentifier[theLeft.size()]);
    }

    public void maybeDead(IbisIdentifier ibisIdentifier) throws IOException {
        for (String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            subRegistry.maybeDead(ibis.mapIdentifier(ibisIdentifier, ibisName));
        }
    }

    public String[] receivedSignals() {
        HashMap<String, String> theSignals = new HashMap<String, String>();
        for (Registry subRegistry:subRegistries.values()) {
            String[] signals = subRegistry.receivedSignals();
            for(int i=0; i<signals.length; i++) {
                theSignals.put(signals[i], signals[i]);
            }
        }
        return theSignals.keySet().toArray(new String[theSignals.size()]);
    }

    public void signal(String signal, IbisIdentifier... ibisIdentifiers)
            throws IOException {
        IbisIdentifier[] ids = new IbisIdentifier[ibisIdentifiers.length];
        for(String ibisName:subRegistries.keySet()) {
            Registry subRegistry = subRegistries.get(ibisName);
            for (int i=0; i<ids.length; i++) {
                ids[i] = ((MultiIbisIdentifier)ibisIdentifiers[i]).subIdForIbis(ibisName);
            }
            subRegistry.signal(signal, ids);
        }
    }

    public void waitUntilPoolClosed() {
        for (Registry subRegistry:subRegistries.values()) {
            subRegistry.waitUntilPoolClosed();
        }
    }

    public String getManagementProperty(String key)
    throws NoSuchPropertyException {
        return ManageableMapper.getManagementProperty(key);
    }

    public Map<String, String> managementProperties() {
        return ManageableMapper.managementProperties();
    }

    public void printManagementProperties(PrintStream stream) {
        ManageableMapper.printManagementProperties(stream);
    }

    public void setManagementProperties(Map<String, String> properties)
    throws NoSuchPropertyException {
        ManageableMapper.setManagementProperties(properties);
    }

    public void setManagementProperty(String key, String value)
    throws NoSuchPropertyException {
        ManageableMapper.setManagementProperty(key, value);
    }

}

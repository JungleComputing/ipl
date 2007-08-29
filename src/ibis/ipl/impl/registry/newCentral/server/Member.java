package ibis.ipl.impl.registry.newCentral.server;

import ibis.ipl.impl.IbisIdentifier;

import org.apache.log4j.Logger;

final class Member {

    private static final Logger logger = Logger.getLogger(Member.class);

    private final IbisIdentifier ibis;

    private int currentTime;
    
    //time at which this member was last seen
    private long lastSeen;

    Member(IbisIdentifier ibis) {
        this.ibis = ibis;
        currentTime = 0;
        lastSeen = 0;
    }

    IbisIdentifier getIbis() {
        return ibis;
    }

    synchronized int getCurrentTime() {
        return currentTime;
    }

    synchronized void setCurrentTime(int currentTime) {
        if (currentTime < this.currentTime) {
            logger.error("tried to set time backwards on member");
            return;
        }
        this.currentTime = currentTime;
    }
    
    synchronized void updateLastSeenTime() {
        lastSeen = System.currentTimeMillis();
    }
    
    synchronized void clearLastSeenTime() {
        lastSeen = 0;
    }
    
    synchronized long getLastSeen() {
        return lastSeen;
    }

    String getID() {
        return ibis.getID();
    }

    @Override
    public String toString() {
        return ibis.toString() + "@T" + getCurrentTime();
    }

}

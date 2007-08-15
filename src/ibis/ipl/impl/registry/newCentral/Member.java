package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.impl.IbisIdentifier;

import org.apache.log4j.Logger;

final class Member {

    private static final Logger logger = Logger.getLogger(Member.class);

    private final IbisIdentifier ibis;

    private int currentTime;

    Member(IbisIdentifier ibis) {
        this.ibis = ibis;
        currentTime = 0;
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

    String getID() {
        return ibis.getID();
    }

    @Override
    public String toString() {
        return ibis.toString() + "@T" + getCurrentTime();
    }
}

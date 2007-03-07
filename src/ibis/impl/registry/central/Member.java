package ibis.impl.registry.central;

import org.apache.log4j.Logger;

import ibis.impl.IbisIdentifier;

public class Member {
    
    private static final Logger logger = Logger.getLogger(Member.class);
    
    private final IbisIdentifier ibis;
    private int currentTime;
    
    Member(IbisIdentifier ibis) {
        this.ibis = ibis;
        currentTime  = 0;
    }
    
    public IbisIdentifier ibis() {
        return ibis;
    }
    
    public synchronized int getCurrentTime() {
        return currentTime;
    }
    
    public synchronized void setCurrentTime(int currentTime) {
        if (currentTime < this.currentTime) {
            logger.error("tried to set time backwards on member");
            return;
        }
        this.currentTime = currentTime;
    }

    public String getID() {
        return ibis.myId;
    }
    
    public String toString() {
        return ibis.toString() + "@T" + getCurrentTime();
    }
}

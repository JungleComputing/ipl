package ibis.ipl.impl.registry.central;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import ibis.ipl.impl.IbisIdentifier;

import org.apache.log4j.Logger;

public final class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Member.class);

    private final IbisIdentifier ibis;

    private final Event event;

    private int currentTime;

    // time at which this member was last seen
    private long lastSeen;

    public Member(IbisIdentifier ibis, Event event) {
        this.ibis = ibis;
        this.event = event;
        currentTime = 0;
        lastSeen = 0;
    }

    public Member(DataInput in) throws IOException {
        ibis = new IbisIdentifier(in);
        event = new Event(in);
        currentTime = 0;
        lastSeen = 0;
    }

    public void writeTo(DataOutput out) throws IOException {
        ibis.writeTo(out);
        event.writeTo(out);
    }

    public IbisIdentifier getIbis() {
        return ibis;
    }

    public synchronized int getCurrentTime() {
        return currentTime;
    }

    public synchronized void setCurrentTime(int currentTime) {
        if (currentTime < this.currentTime) {
            logger.error(
                "tried to set time backwards on member. Current time = "
                        + this.currentTime + " new time = " + currentTime,
                new Exception());
            return;
        }
        this.currentTime = currentTime;
    }

    public synchronized void updateLastSeenTime() {
        lastSeen = System.currentTimeMillis();
    }

    public synchronized void clearLastSeenTime() {
        lastSeen = 0;
    }

    public synchronized long getLastSeen() {
        return lastSeen;
    }

    String getID() {
        return ibis.getID();
    }

    @Override
    public String toString() {
        return ibis.toString() + "@T" + getCurrentTime();
    }

    public Event getEvent() {
        return event;
    }

}

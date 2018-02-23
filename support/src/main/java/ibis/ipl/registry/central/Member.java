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

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Member implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(Member.class);

    private final IbisIdentifier ibis;

    private final Event event;

    private int currentEventTime;

    // field used to keep track of when this member was last seen (server),
    // or when it was last reported to be dead (client) 
    private long time;

    public Member(IbisIdentifier ibis, Event event) {
        this.ibis = ibis;
        this.event = event;
        currentEventTime = 0;
        time = 0;
    }

    public Member(DataInput in) throws IOException {
        ibis = new IbisIdentifier(in);
        event = new Event(in);
        currentEventTime = 0;
        time = 0;
    }

    public void writeTo(DataOutput out) throws IOException {
        ibis.writeTo(out);
        event.writeTo(out);
    }

    public IbisIdentifier getIbis() {
        return ibis;
    }

    public synchronized int getCurrentTime() {
        return currentEventTime;
    }

    public synchronized void setCurrentTime(int currentTime) {
        if (currentTime < this.currentEventTime) {
            logger.error(
                "tried to set time backwards on member. Current time = "
                        + this.currentEventTime + " new time = " + currentTime,
                new Exception());
            return;
        }
        this.currentEventTime = currentTime;
    }

    public synchronized void updateTime() {
        time = System.currentTimeMillis();
    }

    public synchronized void clearTime() {
    	time = 0;
    }

    public synchronized long getTime() {
        return time;
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

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

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventList {

    private static final Logger logger = LoggerFactory.getLogger(EventList.class);

    private final SortedSet<Event> events;

    private int minimum;

    public EventList() {
        events = new TreeSet<>();
        minimum = 0;
    }

    /**
     * Returns the specified event.
     *
     * @param time Time of the requested event.
     *
     * @return the specified event, or null if it is not found.
     */
    public Event get(int time) {
        if (logger.isDebugEnabled()) {
            logger.debug("getting event " + time);
        }
        for (Event event : events) {
            if (event.getTime() == time) {
                if (logger.isDebugEnabled()) {
                    logger.debug("getting event " + time + ", returning " + event);
                }
                return event;
            }
            if (event.getTime() > time) {
                if (logger.isDebugEnabled()) {
                    logger.debug("getting event " + time + ", returning <null>");
                }
                return null;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getting event " + time + ", returning <null>");
        }
        return null;
    }

    public Event[] getList(int start) {
        if (logger.isDebugEnabled()) {
            logger.debug("getting events from " + start);
        }
        ArrayList<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (logger.isDebugEnabled()) {
                logger.debug("considering " + event);
            }
            int time = event.getTime();
            if (time >= start) {
                if (result.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("adding " + event);
                    }
                    result.add(event);
                } else {
                    Event previous = result.get(result.size() - 1);
                    if (time != (previous.getTime() + 1)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("NOT adding " + event + " time should be " + (previous.getTime() + 1));
                        }
                        return result.toArray(new Event[0]);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("adding " + event);
                    }
                    result.add(event);
                }
            }
        }
        return result.toArray(new Event[0]);
    }

    public Event[] getSignalEvents(int from, int to) {
        ArrayList<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getTime() > to) {
                // no more events will have a time >= from and <= to
                return result.toArray(new Event[0]);
            }

            if (event.getType() == Event.SIGNAL && event.getTime() >= from) {
                result.add(event);
            }
        }

        return result.toArray(new Event[0]);
    }

    public void add(Event... newEvents) {
        for (Event event : newEvents) {
            events.add(event);
            if (logger.isDebugEnabled()) {
                logger.debug("adding event to list: " + event + ", list now " + toString());
            }
        }
    }

    public void setMinimum(int time) {
        minimum = time;
        if (logger.isDebugEnabled()) {
            logger.debug("purging upto " + time);
        }
        while (!events.isEmpty() && events.first().getTime() < time) {
            Event first = events.first();
            if (logger.isDebugEnabled()) {
                logger.debug("removing " + first);
            }
            events.remove(first);
        }
    }

    public int getNextRequiredEvent() {
        int result = minimum;

        for (Event event : events) {
            if (event.getTime() == result + 1) {
                result = result + 1;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("next required event: " + result);
        }

        return result;
    }

    @Override
    public String toString() {
        String message = "eventList: ";
        for (Event event : events) {
            message += event + " ";
        }

        return message;
    }

}

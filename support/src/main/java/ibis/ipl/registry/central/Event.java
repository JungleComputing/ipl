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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import ibis.ipl.impl.IbisIdentifier;

public final class Event implements Serializable, Comparable<Event> {

    private static final long serialVersionUID = 1L;

    // event types

    public static final int JOIN = 0;

    public static final int LEAVE = 1;

    public static final int DIED = 2;

    public static final int SIGNAL = 3;

    public static final int ELECT = 4;

    public static final int UN_ELECT = 5;

    public static final int POOL_CLOSED = 6;

    public static final int POOL_TERMINATED = 7;

    public static final int NR_OF_TYPES = 8;

    private final int time;

    private final int type;

    private final String description;

    // single ibis field, denoting which ibis this event is about
    private final IbisIdentifier ibis;

    // multiple ibisses field (only used for destinations in signals)
    private final IbisIdentifier[] destinations;

    public Event(int time, int type, String description, IbisIdentifier ibis, IbisIdentifier... destinations) {
        this.time = time;
        this.type = type;
        this.ibis = ibis;
        this.destinations = destinations.clone();
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }

        if (type != SIGNAL && destinations.length != 0) {
            throw new Error("only the signal type event can have a destination");
        }
    }

    public Event(DataInput in) throws IOException {
        time = in.readInt();
        type = in.readInt();
        description = in.readUTF();
        if (in.readBoolean()) {
            ibis = new IbisIdentifier(in);
        } else {
            ibis = null;
        }

        destinations = new IbisIdentifier[in.readInt()];
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = new IbisIdentifier(in);
        }
    }

    public void writeTo(DataOutput out) throws IOException {
        out.writeInt(time);
        out.writeInt(type);
        out.writeUTF(description);

        if (ibis != null) {
            out.writeBoolean(true);
            ibis.writeTo(out);
        } else {
            out.writeBoolean(false);
        }
        out.writeInt(destinations.length);
        for (IbisIdentifier destination : destinations) {
            destination.writeTo(out);
        }
    }

    public int getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public IbisIdentifier getIbis() {
        return ibis;
    }

    public IbisIdentifier[] getDestinations() {
        return destinations.clone();
    }

    public int getType() {
        return type;
    }

    private String typeString() {
        switch (type) {
        case JOIN:
            return "JOIN";
        case LEAVE:
            return "LEAVE";
        case DIED:
            return "DIED";
        case SIGNAL:
            return "SIGNAL";
        case ELECT:
            return "ELECT";
        case UN_ELECT:
            return "UN_ELECT";
        case POOL_CLOSED:
            return "POOL_CLOSED";
        case POOL_TERMINATED:
            return "POOL_TERMINATED";
        default:
            return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return typeString() + "@" + time;
    }

    @Override
    public int compareTo(Event other) {
        return time - other.time;
    }

}

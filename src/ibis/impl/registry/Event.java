package ibis.impl.registry;

import ibis.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public final class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    // event types

    public static final int JOIN = 1;

    public static final int LEAVE = 2;

    public static final int DIED = 3;

    public static final int SIGNAL = 4;

    public static final int ELECT = 5;

    public static final int UN_ELECT = 6;

    private final int time;

    private final int type;

    private final String description;

    private final IbisIdentifier[] ibisses;

    public Event(int time, int type, IbisIdentifier ibis, String description) {
        this.time = time;
        this.type = type;
        if (ibis == null) {
            this.ibisses = new IbisIdentifier[0];
        } else {
            this.ibisses = new IbisIdentifier[] { ibis };
        }
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }
    }

    public Event(int time, int type, IbisIdentifier[] ibisses,
            String description) {
        this.time = time;
        this.type = type;
        this.ibisses = ibisses.clone();
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }
    }

    public Event(DataInput in) throws IOException {
        time = in.readInt();
        type = in.readInt();
        description = in.readUTF();
        ibisses = new IbisIdentifier[in.readInt()];
        for (int i = 0; i < ibisses.length; i++) {
            ibisses[i] = new IbisIdentifier(in);
        }
    }

    public void writeTo(DataOutput out) throws IOException {
        out.writeInt(time);
        out.writeInt(type);
        out.writeUTF(description);
        out.writeInt(ibisses.length);
        for (int i = 0; i < ibisses.length; i++) {
            ibisses[i].writeTo(out);
        }

    }

    public int getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public IbisIdentifier getFirstIbis() {
        if (ibisses.length == 0) {
            return null;
        }
        return ibisses[0];
    }

    public IbisIdentifier[] getIbisses() {
        return ibisses.clone();
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
        default:
            return "UNKNOWN";
        }
    }

    public String toString() {
        return typeString() + "@" + time;
    }

}

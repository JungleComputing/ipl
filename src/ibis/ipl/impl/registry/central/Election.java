package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

public class Election {
    
    private final Event event;

    public Election(Event event) {
        this.event = event;
    }

    public String getName() {
        return event.getDescription();
    }

    public IbisIdentifier getWinner() {
        return event.getFirstIbis();
    }
    
    public Event getEvent() {
        return event;
    }
    
    public String toString() {
        return getName() + " won by " + getWinner();
    }
}

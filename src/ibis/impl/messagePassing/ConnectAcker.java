package ibis.impl.messagePassing;

public class ConnectAcker extends Syncer {

    private boolean accepted;
    private int	acks = 1;

    public void setAcks(int acks) {
	this.acks = acks;
	this.accepted = false;
    }

    public boolean satisfied() {
	return acks == 0;
    }

    public boolean accepted() {
	return accepted;
    }

    public void signal() {
	if (acks <= 0) {
	    throw new Error(this + ": wakeup but acks " + acks); 
	}
	--acks;
	wakeup();
    }

    public void signal(boolean accepted) {
	this.accepted = accepted;
	signal();
    }

}



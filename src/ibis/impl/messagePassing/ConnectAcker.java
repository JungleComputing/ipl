package ibis.impl.messagePassing;

public class ConnectAcker extends Syncer {

    private boolean accepted;
    private int	acks = 1;

    public void setAcks(int acks) {
	this.acks = acks;
    }

    public boolean satisfied() {
	return acks == 0;
    }

    public boolean accepted() {
	return accepted;
    }

    public void wakeup() {
	--acks;
	super.wakeup();
    }

    public void wakeup(boolean accepted) {
	this.accepted = accepted;
	wakeup();
    }

}



class PandaSigHandler implements kh.signal.SignalListener {

    int mySignal;

    PandaSigHandler(int mySignal) {
	this.mySignal = mySignal;
	kh.signal.SignalManager.addListener(this);
    }

    PandaSigHandler() {
	this(SIGHUP);
    }

    public void signalReceived(int signalNum) {
    }


    public boolean isInterested(int signalNum) {
	return signalNum == mySignal;
    }

}

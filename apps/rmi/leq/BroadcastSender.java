class BroadcastSender extends Thread {

    i_BroadcastObject dest;
    BroadcastObject local;
    Update u;
    int seqno = 0;

    public BroadcastSender(BroadcastObject b, i_BroadcastObject d) {
	dest = d;
	local = b;
	u = new Update();
    }

    private void send() {
	try { 
	    local.get(u, seqno);
	    dest.put(u.update, u.stop);
	    seqno++;
	} catch (Exception e) { 
	    e.printStackTrace();
	    System.exit(1);
	} 
    }

    public void run() {
	while(true) send();
    }
}

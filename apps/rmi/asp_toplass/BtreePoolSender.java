class BtreePoolSender extends Thread {
i_Asp dest;
int[] row;
int k;
int owner;
boolean filled = false;

synchronized void put(i_Asp dest, int[] row, int k, int owner) {
	while(filled) {
		try {
			wait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	filled = true;

	this.dest = dest;
	this.row = row;
	this.k = k;
	this.owner = owner;

	notifyAll();

	// Maybe we need a yield to switch to the sender thread...
	// Last time I checked, it didn't help at all.
	//	Thread.yield();
}


synchronized void send() {
	while(!filled) {
		try {
			wait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	try {
		dest.btree_transfer(row, k, owner);
	} catch (Exception e) {
		System.out.println("Btree pool send failed ! " + e);
		e.printStackTrace();
	}

	filled = false;

	notifyAll();
}


public void run() {
	while(true) send();
}
}

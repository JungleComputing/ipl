package ibis.ipl.impl.net;

public class NetPriorityMutex {

	private int lockvalue     = 1;
	private int priorityvalue = 0;

	public NetPriorityMutex() {
		this(false);
	}

	public NetPriorityMutex(boolean locked) {
		lockvalue = locked?0:1;
	}

	public NetPriorityMutex(boolean priority, boolean locked) {
                priorityvalue = priority?1:0;
		lockvalue     = locked?0:1;
	}

	public synchronized void lock(boolean priority) {
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
				//
                                }
                        }
                } else {
                        while (priorityvalue > 0) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
				//
                                }
                        }
                        while (lockvalue <= 0) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
				//
                                }
                        }
                }
		lockvalue--;
	}

	public synchronized void ilock(boolean priority) throws InterruptedException {
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
                                wait();
                        }
                } else {
                        while (priorityvalue > 0) {
                                wait();
                        }
                        while (lockvalue <= 0) {
                                wait();
                        }
                }
		lockvalue--;
	}

	public synchronized boolean trylock(boolean priority) {
                if (priority) {
                        if (lockvalue <= 0) {
                                return false;
                        }

                        priorityvalue++;
                } else {
                        if (priorityvalue > 0) {
                                return false;
                        }
                        
                        if (lockvalue <= 0) {
                                return false;
                        }
                }

                lockvalue--;
                return true;
	}

	public synchronized void unlock(boolean priority) {
                if (priority) {
                        priorityvalue--;
                }
                
		lockvalue++;
		notifyAll();
	}
}


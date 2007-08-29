package ibis.ipl.impl.registry.newCentral.server;

import ibis.util.ThreadPool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Sends events to clients from the server.
 */
final class IterativeEventPusher implements Runnable {
    
	private class WorkQ {
		private List<Member> q;
		private int count;

		WorkQ(Member[] work) {
			//Arrays.asList list does not support remove, so do this "trick"
			q = new LinkedList<Member>();
			q.addAll(Arrays.asList(work));
			
			count = this.q.size();
		}

		synchronized Member next() {
			if (q.isEmpty()) {
				return null;
			}

			return q.remove(0);
		}

		synchronized void doneJob() {
			count--;

			if (count <= 0) {
				notifyAll();
			}
		}

		synchronized void waitUntilDone() {
			while (count > 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					// IGNORE
				}
			}
		}
	}

	private class EventPusherThread implements Runnable {

		WorkQ workQ;

		EventPusherThread(WorkQ workQ) {
			this.workQ = workQ;

			ThreadPool.createNew(this, "event pusher thread");
		}

		public void run() {
			while (true) {
				Member work = workQ.next();

				if (work == null) {
					// done pushing
					return;
				}

				logger.debug("pushing to " + work);

				pool.push(work, false);
				workQ.doneJob();
			}
		}
	}

	private static final Logger logger = Logger
			.getLogger(IterativeEventPusher.class);

	private final Pool pool;

	private final int threads;
        
        private final long timeout;

	IterativeEventPusher(Pool pool, int threads, long timeout) {
		this.pool = pool;
		this.threads = threads;
                this.timeout = timeout;
		
		ThreadPool.createNew(this, "event pusher scheduler thread");
	}

	public void run() {
		while (!pool.ended()) {
			int eventTime = pool.getEventTime();

			Member[] members = pool.getMembers();

			logger.debug("updating nodes in pool (pool size = "
					+ members.length + "  to event-time " + eventTime);

			WorkQ workQ = new WorkQ(members);

			int threads = Math.min(this.threads, members.length);
			for (int i = 0; i < threads; i++) {
				new EventPusherThread(workQ);
			}

			workQ.waitUntilDone();

			logger.debug("DONE updating nodes in pool to event-time "
					+ eventTime);

			pool.waitForEventTime(eventTime + 1, timeout);
		}
	}

}

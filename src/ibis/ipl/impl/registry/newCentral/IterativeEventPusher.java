package ibis.ipl.impl.registry.newCentral;

import java.util.List;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

/**
 * Sends events to clients from the server.
 */
final class IterativeEventPusher implements Runnable {

	private class WorkQ {
		private List<IbisIdentifier> q;
		private int count;

		WorkQ(List<IbisIdentifier> work) {
			this.q = work;
			count = this.q.size();
		}

		synchronized IbisIdentifier next() {
			if (q.isEmpty()) {
				return null;
			}

			return q.remove(0);
		}

		synchronized void doneJob() {
			count--;
		}

		synchronized boolean isDone() {
			return count == 0;
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
				IbisIdentifier work = workQ.next();

				if (work == null) {
					// done pushing
					return;
				}

				logger.debug("pushing to " + work);

				pool.push(work);
				workQ.doneJob();
			}
		}
	}

	private static final Logger logger = Logger.getLogger(IterativeEventPusher.class);

	private final Pool pool;

	private final int threads;

	private final long interval;
	
	private final boolean newEventTriggersPush;

	IterativeEventPusher(Pool pool, int threads, long interval, boolean newEventTriggersPush) {
		this.pool = pool;
		this.threads = threads;
		this.interval = interval;
		this.newEventTriggersPush = newEventTriggersPush;

		ThreadPool.createNew(this, "event pusher scheduler thread");
	}

	public void run() {
		int minimum = 0;
		int prevMinimum = 0;
		int prevPrevMinimum = 0;
		while (!pool.ended()) {
			int eventTime = pool.getEventTime();
			
			long deadline = System.currentTimeMillis() + interval;

			List<IbisIdentifier> members = pool.getMemberList();

			logger.debug("updating nodes in pool (pool size = "
					+ members.size() + "  to event-time " + eventTime);

			WorkQ workQ = new WorkQ(members);

			int threads = Math.min(this.threads, members.size());
			for (int i = 0; i < threads; i++) {
				new EventPusherThread(workQ);
			}

			synchronized (this) {
				while (!workQ.isDone()) {
					try {
						// wait a while before checking again
						wait(100);
					} catch (InterruptedException e) {
						// IGNORE
					}
				}

				logger.debug("DONE updating nodes in pool to event-time "
						+ eventTime);

				long currentTime = System.currentTimeMillis();
				
				
				// wait until the minimum interval has passed
				if (currentTime < deadline) {
					try {
						wait(deadline - currentTime);
					} catch (InterruptedException e) {
						// IGNORE
					}
				}
			}
			
			prevPrevMinimum = prevMinimum;
			prevMinimum = minimum;
			minimum = eventTime;
			
			pool.purgeUpto(prevPrevMinimum);
		}
	}

}

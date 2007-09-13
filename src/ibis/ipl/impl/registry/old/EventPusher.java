package ibis.ipl.impl.registry.old;

import ibis.util.ThreadPool;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * Sends events to clients from the server.
 */
final class EventPusher implements Runnable {

	private class WorkQ {
		private List<Member> q;
		private int count;

		WorkQ(List<Member> work) {
			this.q = work;
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
				Member work = workQ.next();

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

	private static final Logger logger = Logger.getLogger(EventPusher.class);

	private final Pool pool;

	private final int threads;

	private final long minInterval;

	EventPusher(Pool pool, int threads, long minInterval) {
		this.pool = pool;
		this.threads = threads;
		this.minInterval = minInterval;

		ThreadPool.createNew(this, "event pusher scheduler thread");
	}

	public void run() {
		int minimum = 0;
		int prevMinimum = 0;
		int prevPrevMinimum = 0;
		while (!pool.ended()) {
			int eventTime = pool.getEventTime();
			
			long minEndTime = System.currentTimeMillis() + minInterval;

			List<Member> members = pool.getMemberList();

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
				if (currentTime < minEndTime) {
					try {
						wait(minEndTime - currentTime);
					} catch (InterruptedException e) {
						// IGNORE
					}
				}
			}
			
			prevPrevMinimum = prevMinimum;
			prevMinimum = minimum;
			minimum = eventTime;
			
			pool.purgeUpto(prevPrevMinimum);

			// wait until some event has happened
			// (which it might already have)
			pool.waitForEventTime(eventTime + 1);
		}
	}

}

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Communication extends SpawnSync {

	//used by GlobalResultTable
	static void connect(SendPort s, ReceivePortIdentifier ident) {
		boolean success = false;
		do {
			try {
				s.connect(ident);
				success = true;
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("connecting to: " + ident);			
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
					// ignore
				}
			}
		} while (!success);
	}

	static boolean connect(SendPort s, ReceivePortIdentifier ident,
			long timeoutMillis) {
		boolean success = false;
		long startTime = System.currentTimeMillis();
		do {
			try {
				s.connect(ident, timeoutMillis);
				success = true;
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("connecting to: " + ident);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
					// ignore
				}
			}
		} while (!success
				&& System.currentTimeMillis() - startTime < timeoutMillis);
		return success;
	}

	//used by GlobalResultTable
	ReceivePortIdentifier lookup(String name) throws IOException {
		return ibis.registry().lookup(name);
	}

	SendPort getReplyPortWait(IbisIdentifier ident) {
		SendPort s;
		if (ASSERTS) {
			assertLocked(this);
		}

		do {
			s = victims.getReplyPort(ident);
			if (s == null) {
				if (COMM_DEBUG) {

					out.println("SATIN '" + this.ident.name()
							+ "': could not get reply port to " + ident.name()
							+ ", retrying");
				}
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			}
		} while (s == null);

		return s;
	}

	SendPort getReplyPortNoWait(IbisIdentifier ident) {
		SendPort s;
		if (ASSERTS) {
			assertLocked(this);
		}
		s = victims.getReplyPort(ident);
		return s;
	}

	boolean satinPoll() {
		if (POLL_FREQ == 0) { // polling is disabled
			if (HANDLE_MESSAGES_IN_LATENCY) {
				System.err
						.println("Polling is disabled while messages are handled in the latency.\n"
								+ "This is a configureation error.");
				System.exit(1);
			}
			return false;
		}

		if (upcalls && !upcallPolling) { // we are using upcalls, but don't want
			// to poll
			if (HANDLE_MESSAGES_IN_LATENCY) {
				System.err
						.println("Polling is disabled while messages are handled in the latency.\n"
								+ "This is a configureation error.");
				System.exit(1);
			}
			return false;
		}

		if (POLL_FREQ > 0) {
			long curr = pollTimer.currentTimeNanos();
			if (curr - prevPoll < POLL_FREQ) {
				return false;
			}
			prevPoll = curr;
		}

		if (POLL_TIMING)
			pollTimer.start();

		ReadMessage m = null;
		if (POLL_RECEIVEPORT) {
			try {
				m = receivePort.poll();
			} catch (IOException e) {
				System.err.println("SATIN '" + ident.name()
						+ "': Got Exception while polling: " + e);
			}

			if (m != null) {
				messageHandler.upcall(m);
				try {
					m.finish(); // Finish the message, the upcall does not need
					// to do this.
				} catch (Exception e) {
					System.err.println("error in finish: " + e);
				}
			}
		} else {
			try {
				ibis.poll(); // does not return message, but triggers upcall.
			} catch (Exception e) {
				System.err.println("polling failed, continuing anyway");
			}
		}

		if (POLL_TIMING)
			pollTimer.stop();

		return m != null;
	}

	void handleDelayedMessages() {
		if (ABORTS) {
			if (gotAborts)
				handleAborts();
			if (gotExceptions)
				handleExceptions();
		}
		if (receivedResults)
			handleResults();
		if (gotActiveTuples)
			handleActiveTuples();

		if (FAULT_TOLERANCE) {
			if (gotCrashes)
				handleCrashes();
			if (gotAbortsAndStores)
				handleAbortsAndStores();
			if (gotDelete)
				handleDelete();
		}
	}

	/* Only allowed when not stealing. */
	void barrier() {
		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + "': barrier start");
		}

		// Close the world, no more join and leave upcalls will be received.
		if (!closed) {
			ibis.closeWorld();
		}

		int size;
		synchronized (this) {
			size = victims.size();
		}

		try {
			if (master) {
				for (int i = 0; i < size; i++) {
					ReadMessage r = barrierReceivePort.receive();
					r.finish();
				}

				for (int i = 0; i < size; i++) {
					SendPort s;
					synchronized (this) {
						s = victims.getPort(i);
					}
					WriteMessage writeMessage = s.newMessage();
					writeMessage.writeByte(Protocol.BARRIER_REPLY);
					writeMessage.finish();
				}
			} else {
				WriteMessage writeMessage = barrierSendPort.newMessage();
				writeMessage.finish();

				if (!upcalls) {
					while (!gotBarrierReply/* && !exiting */) {
						satinPoll();
					}
					/*
					 * Imediately reset gotBarrierReply, we know that a reply
					 * has arrived.
					 */
					gotBarrierReply = false;
				} else {
					synchronized (this) {
						while (!gotBarrierReply) {
							try {
								wait();
							} catch (InterruptedException e) {
								// Ignore.
							}
						}

						/*
						 * Imediately reset gotBarrierReply, we know that a
						 * reply has arrived.
						 */
						gotBarrierReply = false;
					}
				}
			}
		} catch (IOException e) {
			System.err
					.println("SATIN '" + ident.name() + "': error in barrier");
			System.exit(1);
		}

		if (!closed) {
			ibis.openWorld();
		}

		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + "': barrier DONE");
		}
	}
}

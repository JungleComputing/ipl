package ibis.satin.impl;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class Termination extends Initialization {
	/**
	 * Called at the end of the rewritten "main", to do a synchronized exit.
	 */
	public void exit() {
		/* send exit messages to all others */
		int size;

		totalTimer.stop();

		if (!closed) {
			ibis.closeWorld();
		}

		if (stats && detailedStats)
			printDetailedStats();

		connectionUpcallsDisabled = true;

		synchronized (this) {
			size = victims.size();
			//System.err.println("victims size: " + size);
		}

		if (master) {
			exiting = true;
			algorithm.exit(); // give the algorithm time to clean up

			for (int i = 0; i < size; i++) {
				try {
					WriteMessage writeMessage;
					synchronized (this) {
						if (COMM_DEBUG) {
							out.println("SATIN '" + ident.name()
									+ "': sending exit message to "
									+ victims.getIdent(i));
						}

						//System.err.println("victims size: " + victims.size()
						// + ",i: " + i);
						writeMessage = victims.getPort(i).newMessage();
					}

					writeMessage.writeByte(Protocol.EXIT);
					writeMessage.finish();
				} catch (IOException e) {
					synchronized (this) {
						System.err
								.println("SATIN: Could not send exit message to "
										+ victims.getIdent(i));
					}
				}
			}

			while (exitReplies != size) {
				satinPoll();
			}

		} else { // send exit ack to master
			SendPort mp = null;

			synchronized (this) {
				mp = getReplyPortWait(masterIdent);
			}

			try {
				WriteMessage writeMessage;
				if (COMM_DEBUG) {
					out.println("SATIN '" + ident.name()
							+ "': sending exit ACK message to " + masterIdent);
				}

				writeMessage = mp.newMessage();
				writeMessage.writeByte(Protocol.EXIT_REPLY);
				if (stats) {
					writeMessage.writeObject(createStats());
				}
				writeMessage.finish();
			} catch (IOException e) {
				synchronized (this) {
					System.err.println("SATIN: Could not send exit message to "
							+ masterIdent);
				}
			}

			algorithm.exit(); //give the algorithm time to clean up
		}

		barrier(); // Wait until everybody agrees to exit.

		if (master && stats)
			printStats();

		//		System.exit(1);

		try {
			if (SUPPORT_TUPLE_MULTICAST) {
				tuplePort.close();
			}
		} catch (Throwable e) {
			System.err.println("tuplePort.close() throws " + e);
		}

		// If not closed, free ports. Otherwise, ports will be freed in leave
		// calls.
		while (true) {
			try {
				SendPort s;

				synchronized (this) {
					if (victims.size() == 0)
						break;

					s = victims.getPort(0);

					if (COMM_DEBUG) {
						out.println("SATIN '" + ident.name()
								+ "': freeing sendport to "
								+ victims.getIdent(0));
					}
					victims.remove(0);
				}

				if (s != null) {
					s.close();
				}

				//				if(COMM_DEBUG) {
				//				  out.println(" DONE");
				//				  }
			} catch (Throwable e) {
				System.err.println("port.close() throws " + e);
			}
		}

		try {
			receivePort.close();

			if (master) {
				barrierReceivePort.close();
			} else {
				barrierSendPort.close();
			}
		} catch (Throwable e) {
			System.err.println("port.close() throws " + e);
		}

		if (FAULT_TOLERANCE) {
			globalResultTable.exit();
		}

		try {
			ibis.end();
		} catch (Throwable e) {
			System.err.println("ibis.end throws " + e);
		}

		if (COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + "': exited");
		}

		// Do a gc, and run the finalizers. Useful for printing statistics in
		// Satin applications.
		// The app should register a shutdownhook. --Rob
		System.gc();
		System.runFinalization();
		//		System.runFinalizersOnExit(true); // depricated

		System.exit(0); // Needed for IBM jit.
	}
}
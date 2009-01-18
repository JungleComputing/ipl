package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

public class ScatteringOutputStream extends OutputStream {
	
	private static Logger logger = Logger.getLogger(ScatteringOutputStream.class);

	static final int INITIAL_CONNECTIONS_SIZE = 8;
		
	private WriteChannel[] connections = new WriteChannel[INITIAL_CONNECTIONS_SIZE];
	int nrOfConnections = 0;
	
	public ScatteringOutputStream() {
		super();
	}
	
	@Override
	void swapBuffers() throws CollectedWriteException {
		CollectedWriteException cwe = null;
		
        if (logger.isDebugEnabled()) {
        	logger.debug("swapBuffers");
		}
		// TODO Auto-generated method stub
		frontBuffer.flip();
		frontBuffer.mark();
		int i = 0;
		while(i < nrOfConnections) {
			if(!connections[i].isOpen()) {
				if(cwe == null) {
					cwe = new CollectedWriteException();
				}
				cwe.add(connections[i], new ClosedChannelException());
				try {
					doRemove(connections[i]);
				} catch (IOException e) {
					//ignore
				}
			} else {
				try {
					connections[i].flush();
					connections[i].post(frontBuffer);
					frontBuffer.reset();
					i++;
				} catch (IOException e) {
					frontBuffer.reset();
					if(cwe == null) {
						cwe = new CollectedWriteException();
					}
					cwe.add(connections[i], e);
					try {
						doRemove(connections[i]);
					} catch (IOException e1) {
						//ignore
					}
				}
			}
		}

		ByteBuffer temp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = temp;
		frontBuffer.clear();
		if(cwe != null) {
			throw cwe;
		}
	}

	@Override
	void doFlush() throws CollectedWriteException {
		CollectedWriteException cwe = null;
		frontBuffer.flip();
		frontBuffer.mark();
        if (logger.isDebugEnabled()) {
        	logger.debug("Flushing " + frontBuffer.remaining() + " bytes to " + nrOfConnections + " channels");
		}
        
		int i = 0;
		while(i < nrOfConnections) {
			if(!connections[i].isOpen()) {
				if(cwe == null) {
					cwe = new CollectedWriteException();
				}
				cwe.add(connections[i], new ClosedChannelException());
				try {
					doRemove(connections[i]);
				} catch (IOException e) {
					//ignore
				}
			} else {
				try {
					connections[i].post(frontBuffer);
					frontBuffer.reset();
					i++;
				} catch (IOException e) {
					frontBuffer.reset();
					if(cwe == null) {
						cwe = new CollectedWriteException();
					}
					cwe.add(connections[i], e);
					try {
						doRemove(connections[i]);
					} catch (IOException e1) {
						//ignore
					}
				}
			}
		}
		
		
		i = 0;
		while(i < nrOfConnections) {
			try {
				connections[i].flush();
				i++;
			} catch (IOException e) {
				if(cwe == null) {
					cwe = new CollectedWriteException();
				}
				cwe.add(connections[i], e);
				try {
					doRemove(connections[i]);
				} catch (IOException e2) {
					// ignore
				}
			}
		}
		
		frontBuffer.clear();
		backBuffer.clear();
		if(cwe != null) {
			throw cwe;
		}
	}
	
	@Override
	void doClose() {
		while(nrOfConnections > 0) {
			if(connections[0].isOpen()) {
				try {
					connections[0].close();
				} catch(IOException e) {
					//ignore
				}
			}
			try {
				doRemove(connections[0]);
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	public synchronized void add(WriteChannel connection) {
		// end all current transfers
		try {
			flush();
		} catch (IOException e) {
			// well, that stream is already closed, I think
			// TODO filter for ClosedConnectionException or something
		}
	
		if (nrOfConnections == connections.length) {
            WriteChannel[] newConnections = new WriteChannel[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = connection;
        if (logger.isDebugEnabled()) {
        	logger.debug("Connection added at position " + nrOfConnections);
		}
        
        nrOfConnections++;	
	}

	public synchronized void remove(WriteChannel connection) throws IOException {
		//logger.debug("remove");
		// end all current transfers
		try {
			flush();
		} catch (IOException e) {
			// well, stream is already closed, I think
			// TODO filter for ClosedConnectionException or something
		}
		if(nrOfConnections == 0) {
			throw new IOException("no connection to remove");
		}
		doRemove(connection);
    }
	
	private synchronized void doRemove(WriteChannel connection) throws IOException {
        for (int i = 0; i < nrOfConnections; i++) {
            if (connections[i] == connection) {
                if (logger.isDebugEnabled()) {
                	logger.debug("Connection removed at position " + i);
                }
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                if (nrOfConnections == 0) {
                	// all connection are gone
                	closed = true;
                }
                return;
            }
        }
        
        throw new IOException("tried to remove non existing connections");
    }
	
	
}

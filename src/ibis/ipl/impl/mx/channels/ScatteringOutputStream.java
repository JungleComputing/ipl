package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	void swapBuffers() throws IOException {
        if (logger.isDebugEnabled()) {
        	logger.debug("swapBuffers");
		}
		// TODO Auto-generated method stub
		frontBuffer.flip();
		frontBuffer.mark();
		for(int i = 0; i< nrOfConnections; i++) {
			connections[i].flush();
			connections[i].post(frontBuffer);
			frontBuffer.reset();
		}

		ByteBuffer temp = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = temp;
		frontBuffer.clear();
	}

	@Override
	void doFlush() throws IOException {
		frontBuffer.flip();
		frontBuffer.mark();
        if (logger.isDebugEnabled()) {
        	logger.debug("Flushing " + frontBuffer.remaining() + " bytes");
		}
		for(int i = 0; i< nrOfConnections; i++) {
			connections[i].post(frontBuffer);
			frontBuffer.reset();
		}
		for(int i = 0; i< nrOfConnections; i++) {
			connections[i].flush();
		}
		frontBuffer.clear();
		backBuffer.clear();
	}
	
	@Override
	void doClose() throws IOException {
		for(int i = 0; i< nrOfConnections; i++) {
			connections[i].close();
		}
	}


	public synchronized void add(WriteChannel connection) {
		// end all current transfers
		try {
			flush();
		} catch (IOException e) {
			// well, stream is already closed, I think
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

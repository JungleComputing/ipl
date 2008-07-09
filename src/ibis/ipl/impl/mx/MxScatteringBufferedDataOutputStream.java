package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

public class MxScatteringBufferedDataOutputStream extends MxBufferedDataOutputStream {

	static final int INITIAL_CONNECTIONS_SIZE = 8;
	private static Logger logger = Logger.getLogger(MxScatteringBufferedDataOutputStream.class);
	
	private WriteChannel[] connections = new WriteChannel[INITIAL_CONNECTIONS_SIZE];
	int nrOfConnections = 0;
	
	public MxScatteringBufferedDataOutputStream() {
		super();
	}
	
	@Override
	protected void doClose() throws IOException {
		for(int i = 0; i< nrOfConnections; i++) {
			//logger.debug("close " + i);
			connections[i].close();	
		}
	}

	@Override
	protected void doFinish() throws IOException {
		for(int i = 0; i< nrOfConnections; i++) {
			//logger.debug("finish " + i);
			connections[i].finish();
			//FIXME catch and stack exceptions
		}
		buffer.clear();
	}

	@Override
	protected boolean doFinished() throws IOException {
		for(int i = 0; i< nrOfConnections; i++) {
			//logger.debug("poll " + i);
			if( !connections[i].isFinished()) { //FIXME catch exceptions
				return false;
			}
		}
		buffer.clear();
		return true;
	}

	@Override
	protected long doWrite() throws IOException {
		buffer.flip();
		for(int i = 0; i< nrOfConnections; i++) {
			//FIXME catch exceptions
			//logger.debug("flush " + i);
			connections[i].write(buffer);
		}
		return buffer.remaining();
	}

	protected synchronized void add(WriteChannel connection) {
		// end all current transfers
		try {
			finish();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	
		if (nrOfConnections == connections.length) {
            WriteChannel[] newConnections = new MxWriteChannel[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = connection;
        //logger.debug("Connection added at position " + nrOfConnections);
        nrOfConnections++;	
	}

	protected synchronized void remove(WriteChannel connection) throws IOException {
		//logger.debug("remove");
		// end all current transfers
		try {
			finish();
		} catch (IOException e) {
			// well, stream is already closed, I think
			// TODO filter for ClosedConnectionException or something
		}
		if(nrOfConnections == 0) {
			throw new IOException("no connection to remove");
		}
        for (int i = 0; i < nrOfConnections; i++) {
            if (connections[i] == connection) {
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                return;
            }
        }
        throw new IOException("tried to remove non existing connections");
    }
	
}

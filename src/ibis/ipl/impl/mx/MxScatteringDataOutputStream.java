package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

public class MxScatteringDataOutputStream extends MxDataOutputStream {

	static final int INITIAL_CONNECTIONS_SIZE = 8;
	private ArrayBlockingQueue<SendBuffer> queue;
	private static Logger logger = Logger.getLogger(MxScatteringDataOutputStream.class);
	
	private WriteChannel[] connections = new WriteChannel[INITIAL_CONNECTIONS_SIZE];
	int nrOfConnections = 0;
	
	public MxScatteringDataOutputStream() {
		super();
		queue = new ArrayBlockingQueue<SendBuffer>(Config.FLUSH_QUEUE_SIZE + 1);
	}
	
	public MxScatteringDataOutputStream(WriteChannel channel) {
		this();
		logger.debug("writechannelconstructor");
		add(channel);
	}
	
	@Override
	protected synchronized void doClose() throws IOException {	
		for(int i = 0; i< nrOfConnections; i++) {
			// Do not remove connections here, that is up to the object that added the connection
			//logger.debug("close " + i);
			connections[i].close();
		}
	}

	@Override
	protected synchronized long doSend(SendBuffer buffer) throws IOException {
		buffer.flip();
		for(int i = 0; i< nrOfConnections; i++) {
			//FIXME catch exceptions
			//logger.debug("flush " + i);
			connections[i].write(buffer);
			logger.debug("sent: l[" + buffer.longs.position() + "] d["
                    + buffer.doubles.position() + "] i[" + buffer.ints.position() + "] f["
                    + buffer.floats.position() + "] s[" + buffer.shorts.position() + "] c["
                    + buffer.chars.position() + "] b[" + buffer.bytes.position() + "]");
		}
		//FIXME queue full. Empty the queue?
		while(queue.offer(buffer) == false) {
			/* TODO due to the size of the flush buffer at the channel level, 
			 * the channels will have flushed the first buffer of the queue when this queue is full.
			 * Use this information
			 */
			//flush(); testing this solution
			SendBuffer.recycle(queue.remove());
		}
		return buffer.remaining();
	}
	
	@Override
	protected synchronized void doFlush() throws IOException {
		for(int i = 0; i< nrOfConnections; i++) {
			logger.debug("finish " + i);
			connections[i].flush();
			//FIXME catch and stack exceptions, and recycle buffers
		}
		for(SendBuffer buf : queue) {
			SendBuffer.recycle(buf);
		}
		queue.clear();
	}
	
	@Override
	protected synchronized void doFinish() throws IOException {
		// empty implementation
	}

	@Override
	protected synchronized boolean doFinished() throws IOException {
		// empty implementation
		return true;
	}

	protected synchronized void add(WriteChannel connection) {
		// end all current transfers
		try {
			flush();
		} catch (IOException e) {
			// well, stream is already closed, I think
			// TODO filter for ClosedConnectionException or something
		}
	
		if (nrOfConnections == connections.length) {
            WriteChannel[] newConnections = new MxWriteChannel[connections.length * 2];
            for (int i = 0; i < connections.length; i++) {
                newConnections[i] = connections[i];
            }
            connections = newConnections;
        }
        connections[nrOfConnections] = connection;
        logger.debug("Connection added at position " + nrOfConnections);
        nrOfConnections++;	
	}

	protected synchronized void remove(WriteChannel connection) throws IOException {
		logger.debug("remove");
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
            	logger.debug("Connection removed at position " + i);
                nrOfConnections--;
                connections[i] = connections[nrOfConnections];
                connections[nrOfConnections] = null;
                return;
            }
        }
        throw new IOException("tried to remove non existing connections");
    }	
}

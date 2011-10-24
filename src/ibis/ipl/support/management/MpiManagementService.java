package ibis.ipl.support.management;

import ibis.ipl.impl.Ibis;
import ibis.util.ThreadPool;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiManagementService implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(MpiManagementService.class);
	
	private final ServerSocketChannel mpiManagementServer;
	private final Ibis ibis;
	private static final int CAPACITY = 5*32;	
	private static final int PORT = 6873;
	
	private static final int MAGIC = 654168633;
	private static final int FUNC_SEND = 0;
	private static final int FUNC_SET_SRC = 1;
	
	private final ByteBuffer dst = ByteBuffer.allocate(CAPACITY);
	
    private boolean ended = false;
    
    
	private SocketChannel channel;
    
    public MpiManagementService(Ibis ibis) throws IOException {
    	logger.debug("MPI Management service created.");
		this.ibis = ibis;
			
		mpiManagementServer = ServerSocketChannel.open();		
		mpiManagementServer.socket().bind(new InetSocketAddress(PORT));
				
		ThreadPool.createNew(this, "Management Client - MPI socket interface");
	}
	
	public void run() {
		logger.debug("MPI Management service initialized.");
		
        while (!ended) {        	
        	try {
        		channel = mpiManagementServer.accept();
        		        		
        		channel.read(dst);
        		dst.rewind();
        		IntBuffer buf = dst.asIntBuffer();
        		        		
        		int id = buf.get();
        		int func = buf.get();
        		int mpi_id_to = buf.get();
        		int sent = buf.get();
        		
        		if (id == MAGIC) {
        			if (func == FUNC_SEND) {
        				logger.debug("MPI value caught:" +sent);        				
        				addMPISentBytes(mpi_id_to, sent);
            		} else if (func == FUNC_SET_SRC) {
        				setSourceMPI(mpi_id_to);
        			} else {
            			logger.debug("Got unrecognized function int: "+func);
            		} 
        		} else {
        			buf.rewind();
        			logger.debug("Someone tried to connect illegally! : "+buf.get()+" "+buf.get()+" "+buf.get()+" "+buf.get());
        		}
			} catch (IOException e) {
				logger.error("IOException while trying to open socket.");
			}
        }
    }
	
	void setSourceMPI(int mpi_id) {
		ibis.setMPIid(mpi_id);
	}
	
	void addMPISentBytes(int to_mpi_id, int amount) {
		ibis.addMPISentPerIbis((long)amount, to_mpi_id);
	}

	public void end() {
        synchronized (this) {
            ended = true;
            notifyAll();
        }
        try {
        	mpiManagementServer.close();
        } catch (Exception e) {
            // IGNORE
        }
    }
	
}

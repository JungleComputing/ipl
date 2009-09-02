package ibis.ipl.impl.bt;

import java.io.IOException;

import javax.bluetooth.LocalDevice;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.Connector;

/**
 * Wrapper to expose a bluetooth connection as a socket
 */
class IbisSocket {
   
    StreamConnection streamConnection;
    java.io.OutputStream ostream;
    java.io.InputStream istream;
    String connectionURL;
    
    IbisSocket() {
    	streamConnection = null;
        istream = null; 
        ostream = null;
    }
    
    IbisSocket(StreamConnection newConn) {
    	streamConnection = newConn;
    	try{
    		istream = newConn.openInputStream(); 
        	ostream = newConn.openOutputStream();
    	}catch(IOException e){
    		System.err.println("Unable to open io streams");
    	}
    }    

    void connect(IbisSocketAddress url) throws IOException{
    	//TODO: remove infinite loop and parameterize it via a -D directive
    	streamConnection = null;
    	int i=0;
    	while(streamConnection == null){
    		connectionURL = url.toString();
    		try{
    			//System.out.println("C=> [" + connectionURL + "]...");
    			streamConnection=(StreamConnection)Connector.open(connectionURL);
    		}catch(Exception e){
    			//e.printStackTrace();
    			streamConnection = null;
    			try{
    				i++;
    				int sleep = (int)((250+500*i)*Math.random());
    				//System.out.println("BtFail#" + i + " will sleep for " + sleep + "ms [" + e.toString() + "]" );
    				//System.out.print("[F" + i + "]" );
    				Thread.sleep(sleep);
    			}catch(Exception e2){}
    		}
    	}
    	//System.out.print("[OK]" );
    	ostream = streamConnection.openOutputStream();
    	istream = streamConnection.openInputStream();
    	//System.out.println("OK connect");
    }
    
    void setTcpNoDelay(boolean val) throws IOException {}

    java.io.OutputStream getOutputStream() throws IOException {
        return ostream;
    }

    java.io.InputStream getInputStream() throws IOException {
        return istream;
    }

    int getLocalPort() {
        return 0;
    }

    void close() throws java.io.IOException {
    	
    	try{
    		ostream.flush();
    	}catch(Exception e){
    		//System.out.println("Error on flush [" + e.toString() + "]");    
    	}
    	
        try {
        	ostream.close();
        	istream.close();
        	streamConnection.close();
        }catch(Exception e){
        	//System.out.println("Error on close [" + e.toString() + "]");
        	//throw new java.io.IOException();
        }
        finally {
        	streamConnection = null;
        	ostream = null;
        	istream = null;
        }
    }

    IbisSocketAddress getAddress() {
//        if (socket != null) {
//            return new IbisSocketAddress(socket.getLocalSocketAddress());
//        /}
//        return new IbisSocketAddress(smartSocket.getLocalSocketAddress());
        return null;
    }

    public String toString() {
        return streamConnection.toString();
    }
}

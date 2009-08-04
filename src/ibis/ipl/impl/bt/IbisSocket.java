package ibis.ipl.impl.bt;

import java.io.IOException;
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
        istream = null; 
        ostream = null;
    }    

    void connect(IbisSocketAddress url) throws IOException{
    	//TODO: remove infinite loop and parameterize it via a -D directive
    	streamConnection = null;
    	int i=0;
    	while(streamConnection == null){
    		connectionURL = url.toString();
    		try{
    			//System.out.print("C=> [" + connectionURL + "]...");
    			streamConnection=(StreamConnection)Connector.open(connectionURL);
    		}catch(Exception e){
    			//e.printStackTrace();
    			streamConnection = null;
    			try{
    				i++;
    				int sleep = (int)((500*i)*Math.random());
    				//System.out.println("FAIL #" + i + " will sleep for " + sleep + "ms");
    				Thread.sleep(sleep);
    			}catch(Exception e2){}
    		}
    	}
    	//System.out.println("OK");
    }
    
    void setTcpNoDelay(boolean val) throws IOException {}

    java.io.OutputStream getOutputStream() throws IOException {
    	if(ostream == null)
    		ostream = streamConnection.openOutputStream();
        return ostream;
    }

    java.io.InputStream getInputStream() throws IOException {
    	if(istream == null)
    		istream = streamConnection.openInputStream();        
        return istream;
    }

    int getLocalPort() {
      //  if (socket != null) {
    //        return socket.getLocalPort();
  //      }
//        return smartSocket.getLocalPort();
        return 0;
    }

//    int getPort() {
      //  if (socket != null) {
      //      return socket.getPort();
      //  }
      //  return smartSocket.getPort();
  //  	return 0;
 //   }

    void close() throws java.io.IOException {
        try {
        	streamConnection.close();
        } finally {
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

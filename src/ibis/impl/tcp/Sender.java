package ibis.ipl.impl.tcp;

import ibis.ipl.WriteMessage;

import java.io.OutputStream;
import java.io.IOException;

abstract class Sender {
	abstract ibis.ipl.WriteMessage newMessage() throws IOException;
	abstract void connect(TcpReceivePortIdentifier ri, OutputStream sout, int id) throws IOException;
	abstract void free();	
	abstract void reset() throws IOException;
}

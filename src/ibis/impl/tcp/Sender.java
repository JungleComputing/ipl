package ibis.ipl.impl.tcp;

import ibis.ipl.WriteMessage;
import ibis.ipl.IbisIOException;

import java.io.OutputStream;
import java.io.IOException;

abstract class Sender {
	abstract WriteMessage newMessage() throws IbisIOException;
	abstract void connect(TcpReceivePortIdentifier ri, OutputStream sout, int id) throws IOException;
	abstract void free();	
}

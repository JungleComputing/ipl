package ibis.ipl.impl.tcp;

import ibis.ipl.WriteMessage;
import ibis.ipl.IbisException;

import java.io.OutputStream;
import java.io.IOException;

import ibis.ipl.IbisException;


abstract class Sender {
	abstract WriteMessage newMessage() throws IbisException;
	abstract void connect(TcpReceivePortIdentifier ri, OutputStream sout, int id) throws IOException;
	abstract void free();	
}

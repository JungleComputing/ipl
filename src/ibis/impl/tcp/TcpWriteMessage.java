package ibis.impl.tcp;

import ibis.ipl.WriteMessage;

import java.io.IOException;
import ibis.util.*;
import ibis.io.SerializationOutputStream;
import ibis.ipl.SendPort;

final class TcpWriteMessage implements WriteMessage {
	private TcpSendPort sport;
	private SerializationOutputStream out;
	private boolean connectionAdministration;

	TcpWriteMessage(TcpSendPort p, SerializationOutputStream out,
					boolean connectionAdministration) throws IOException {
		this.connectionAdministration = connectionAdministration;
		sport = p;
		this.out = out;
	}

	// if we keep connectionAdministration, forward exception to upcalls / downcalls.
	// otherwise, rethrow the exception to the user.
	private void forwardLosses(SplitterException e) throws IOException {
		System.err.println("connection lost!");
		if(connectionAdministration) {
			for(int i=0; i<e.count(); i++) {
				sport.lostConnection(e.getStream(i), e.getException(i));
			}
		} else {
			throw e;
		}
	}

	public SendPort localPort() {
		return sport;
	}

	public long getCount() {
		return sport.getCount();
	}

	public void resetCount() {
		sport.resetCount();
	}

	public void send() throws IOException {
	}

	public void finish() throws IOException {
		reset(false);
		sport.finishMessage();
	}

	public void reset(boolean doSend) throws IOException {
		if (doSend) {
			send();
			try {
				out.reset();
			} catch (SplitterException e) {
				forwardLosses(e);
			}

			try {
				out.flush();
			} catch (SplitterException e) {
				forwardLosses(e);
			}
			sport.reset();
		} else {
			try {
				out.reset();
			} catch (SplitterException e) {
				forwardLosses(e);
			}
			try {
				out.flush();
			} catch (SplitterException e) {
				forwardLosses(e);
			}
		}
	}

	public void writeBoolean(boolean value) throws IOException {
		try {
			out.writeBoolean(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeByte(byte value) throws IOException {
		try {
			out.writeByte(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeChar(char value) throws IOException {
		try {
			out.writeChar(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeShort(short value) throws IOException {
		try {
			out.writeShort(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeInt(int value) throws IOException {
		try {
			out.writeInt(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeLong(long value) throws IOException {
		try {
			out.writeLong(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeFloat(float value) throws IOException {
		try {
			out.writeFloat(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeDouble(double value) throws IOException {
		try {
			out.writeDouble(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeString(String value) throws IOException {
		try {
			out.writeUTF(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeObject(Object value) throws IOException {
		try {
			out.writeObject(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(boolean [] value) throws IOException {
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}
	
	public void writeArray(byte [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(char [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(short [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(int [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(long [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(float [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(double [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(Object [] value) throws IOException { 
		try {
			out.writeArray(value);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(boolean [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(byte [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(char [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(short [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(int [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(long [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(float [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(double [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}

	public void writeArray(Object [] value, int offset, int size) throws IOException { 
		try {
			out.writeArray(value, offset, size);
		} catch (SplitterException e) {
			forwardLosses(e);
		}
	}
}

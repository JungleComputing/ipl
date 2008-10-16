package ibis.ipl.impl;

public interface IbisMBean {
	
	public String getIdentifier();
	public long getOutgoingMessageCount();
	public long getBytesWritten();
	public long getBytesSend();
	public long getIncomingMessageCount();
	public long getBytesReceived();
	public long getBytesRead();
	

}

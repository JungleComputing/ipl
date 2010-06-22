package ibis.ipl.impl.stacking.p2p.endtoend;

import java.io.Serializable;

import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;

public class P2PMessage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private int seqNum;
	private byte[] content;
	private int length;
	private SendPortIdentifier sid;
	private ReceivePortIdentifier rid;

	public P2PMessage(byte[] content, int length, SendPortIdentifier sid) {
		this.setContent(content);
		this.setLength(length);
		this.setSid(sid);
		this.setRid(rid);
	}

	/**
	 * @param content
	 *            the content to set
	 */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * @return the content
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * @param length
	 *            the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @param seqNum
	 *            the seqNum to set
	 */
	public void setSeqNum(int seqNum) {
		// set sequence number for this connection
		/*
		 * Iterator it = connections.entrySet().iterator(); while (it.hasNext())
		 * { Map.Entry pairs = (Map.Entry)it.next(); ReceivePortIdentifier[]
		 * receivers = (ReceivePortIdentifier[]) pairs.getValue();
		 * 
		 * for (ReceivePortIdentifier receiver : receivers) { if
		 * (receiver.equals(rid)) { receiver.setSeqNum(seqNum); } } }
		 */

		this.seqNum = seqNum;
	}

	/**
	 * @return the seqNum
	 */
	public int getSeqNum() {
		return seqNum;
	}

	/**
	 * @param sid
	 *            the sid to set
	 */
	public void setSid(SendPortIdentifier sid) {
		this.sid = sid;
	}

	/**
	 * @return the sid
	 */
	public SendPortIdentifier getSid() {
		return sid;
	}

	public void setRid(ReceivePortIdentifier rid) {
		this.rid = rid;
	}

	public ReceivePortIdentifier getRid() {
		return rid;
	}

	public P2PMessage clone() {
		try {
			P2PMessage result =  (P2PMessage) super.clone();
			result.setSid(this.sid);
			result.setRid(this.rid);
			return result;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}

package ibis.ipl.impl.net.muxer.udp;

import java.net.InetAddress;

import ibis.ipl.impl.net.muxer.MuxerKey;

class UdpMuxerKey extends MuxerKey {

    InetAddress		remoteAddress;
    int			remotePort;
    int			remoteKey;

    UdpMuxerKey(Integer spn,
		InetAddress remoteAddress,
		int remotePort,
		int remoteKey) {

	super(spn);

	this.remoteAddress = remoteAddress;
	this.remotePort = remotePort;
	this.remoteKey = remoteKey;
    }

}


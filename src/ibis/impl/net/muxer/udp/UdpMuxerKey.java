package ibis.ipl.impl.net.muxer.udp;

import java.net.InetAddress;

import ibis.ipl.impl.net.muxer.MuxerKey;

class UdpMuxerKey extends MuxerKey {

    InetAddress		remoteAddress;
    int			remotePort;
    int			remoteKey;

    UdpMuxerKey(InetAddress remoteAddress,
		int remotePort,
		int remoteKey) {

	super();

	this.remoteAddress = remoteAddress;
	this.remotePort = remotePort;
	this.remoteKey = remoteKey;
    }

}


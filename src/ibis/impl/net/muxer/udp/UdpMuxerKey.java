package ibis.ipl.impl.net.muxer.udp;

import java.net.InetAddress;

import ibis.ipl.impl.net.muxer.MuxerKey;

class UdpMuxerKey extends MuxerKey {

    InetAddress		remoteAddress;
    int			remotePort;

    UdpMuxerKey(InetAddress remoteAddress,
		int remotePort,
		int remoteKey) {

	super(remoteKey);

	this.remoteAddress = remoteAddress;
	this.remotePort = remotePort;
    }

}


package ibis.impl.net.muxer.udp;

import ibis.impl.net.muxer.MuxerKey;

import java.net.InetAddress;

class UdpMuxerKey extends MuxerKey {

    InetAddress remoteAddress;

    int remotePort;

    UdpMuxerKey(InetAddress remoteAddress, int remotePort, int remoteKey) {

        super(remoteKey);

        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

}


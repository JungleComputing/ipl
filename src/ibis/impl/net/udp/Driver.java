package ibis.ipl.impl.net.udp;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIbis;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetOutput;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

public class Driver extends NetDriver {

	private final String name = "udp";


	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	public String getName() {
		return name;
	}

	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
		return new UdpInput(sp, this, input);
	}

	public NetOutput newOutput(StaticProperties sp,
				   NetOutput	    output)
		throws IbisIOException {
		return new UdpOutput(sp, this, output);
	}
}

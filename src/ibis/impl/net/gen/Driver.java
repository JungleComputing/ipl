package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIbis;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetPoller;
import ibis.ipl.impl.net.NetReceivePortIdentifier;
import ibis.ipl.impl.net.NetSplitter;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

// Generic splitter/poller driver
public class Driver extends NetDriver {
	private final String name = "gen";

	
	public Driver(NetIbis ibis) {
		super(ibis);
	}

	public String getName() {
		return name;
	}

	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
		return new NetPoller(sp, this, input);
	}

	public NetOutput newOutput(StaticProperties sp,
				   NetOutput        output)
		throws IbisIOException {
		return new NetSplitter(sp, this, output);
	}
}

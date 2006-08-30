/* $Id$ */

import java.io.Serializable;
import java.io.IOException;
import java.net.InetAddress;

public final class Data implements Serializable { 
    InetAddress address;
    String name;

    Data(String name, InetAddress me) { 
	this.name = name;
	this.address = me;
    }
} 

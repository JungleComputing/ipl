package ibis.ipl.impl.net.multi.plugins;

import ibis.ipl.impl.net.*;
import ibis.ipl.impl.net.multi.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;


/**
 * Provide an example of multiprotocol's driver plugin.
 */
public final class cluster implements MultiPlugin {


        /**
         * Store the network names the current node belongs to, for indexed lookup.
         */
        static 	Vector   	netVector 	= null;

        /**
         * Store the network names the current node belongs to, for associative lookup.
         */
        static 	Hashtable 	netTable  	= null;

        /**
         * Construct the plugin.
         */
        public cluster() {}

        static {
                Properties 	p   		= System.getProperties();
                InetAddress 	localhost 	= null;

                try {
                        localhost = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                        throw new Error(e);
                }

                String     	chn		= localhost.getCanonicalHostName();
                String     	hn		= localhost.getHostName();

                //System.err.println("canonical local host name: "+chn);
                //System.err.println("local host name: "+hn);

                String  	filename 	= p.getProperty("netibis.multi.cluster.config.filename");
                NetFile 	in       	= null;

                if (filename == null) {
                        filename = "net_multi_cluster_topology.txt";

                        try {
                                in = new NetFile(filename);
                        } catch (FileNotFoundException e) {
                                //System.err.println("no such file ["+filename+"]");
                                in = null;
                        } catch (IOException e) {
                                throw new Error(e);
                        }

                        if (in == null) {
                                String 	userhome      	= p.getProperty("user.home");
                                String 	fileseparator 	= p.getProperty("file.separator");

                                if (userhome != null) {
                                        if (fileseparator == null) {
                                                fileseparator = "/";
                                        }

                                        filename = userhome+fileseparator+filename;
                                        //System.err.println("trying filename = ["+filename+"]");

                                        try {
                                                in = new NetFile(filename);
                                        } catch (FileNotFoundException e) {
                                                //System.err.println("no such file ["+filename+"]");
                                                in = null;
                                        } catch (IOException e) {
                                                throw new Error(e);
                                        }
                                }
                        }

                } else {
                        try {
                                in = new NetFile(filename);
                        } catch (FileNotFoundException e) {
                                in = null;
                        } catch (IOException e) {
                                throw new Error(e);
                        }
                }

                netVector = new Vector();
                netTable  = new Hashtable();

                // Read the file here
                if (in != null) {

                        net_loop:
                        while (true) {
                                try {
                                        // .. Read net name
                                        String name = in.readline();

                                        if (name == null) {
                                                //System.err.println("EOF reached");
                                                break net_loop;
                                        }

                                        name = in.cleanSpaces(name);

                                        if (name.equals("")) {
                                                continue net_loop;
                                        }

                                        int idx = name.indexOf(":");
                                        if (idx < 1) {
                                                throw new Error("syntax error in nework name specification at "+filename+":"+(in.lineNumber()-1));
                                        }

                                        name = name.substring(0, idx);

                                        //System.err.println("network name = ["+name+"]");

                                        // .... Read hosts
                                        boolean added = false;

                                        host_loop:
                                        while (true) {
                                                String host = in.readline();
                                                if (host == null) {
                                                        //System.err.println("EOF reached");
                                                        break net_loop;
                                                }

                                                host = in.cleanSpaces(host);

                                                //System.err.println("host name = ["+host+"]");

                                                if (host.equals("")) {
                                                        break host_loop;
                                                }

                                                if (!added && (host.equals(chn) || host.equals(hn))) {
                                                        //System.err.println("Adding membership to network ["+name+"]");
                                                        netVector.add(name);
                                                        netTable.put(name, name);
                                                        added = true;
                                                }
                                        }
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }
        }


        /**
         * Return a context string according to the networks both sides belong to.
         */
        public String getSubContext(boolean 		isOutgoing,
                                    NetIbisIdentifier  	localId,
                                    NetIbisIdentifier  	remoteId,
                                    ObjectOutputStream	os,
                                    ObjectInputStream 	is) throws NetIbisException {
                try {
                        if (isOutgoing) {
                                Iterator i = netVector.iterator();

                                while (i.hasNext()) {
                                        String s = (String)i.next();
                                        os.writeObject(s);
                                        os.flush();
                                        Boolean b = (Boolean)is.readObject();
                                        if (b.booleanValue()) {
                                                //System.err.println("Outgoing: subcontext string = ["+s+"]");
                                                return s;
                                        }
                                }

                                os.writeObject("");
                                os.flush();
                                return null;
                        } else {
                                while (true) {
                                        String s = (String)is.readObject();
                                        if (s.equals("")) {
                                                return null;
                                        }

                                        if (netTable.get(s) != null) {
                                                os.writeObject(new Boolean(true));
                                                os.flush();
                                                //System.err.println("Incoming: subcontext string = ["+s+"]");
                                                return s;
                                        }
                                        os.writeObject(new Boolean(false));
                                        os.flush();
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new NetIbisException(e);
                }
        }
}


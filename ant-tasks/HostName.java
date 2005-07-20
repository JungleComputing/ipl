/* $Id$ */

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.net.InetAddress;

public class HostName extends Task {

    private final static boolean DEBUG = false;

    private String hostname; // What is this used for?

    private String domainname; // What is this used for?

    private String domainName(String hostName) {
        int dot = hostName.indexOf('.');
        if (dot == -1) {
            return null;
        }

        return hostName.substring(dot + 1);
    }

    public void execute() throws BuildException {
        String hostName = null;
        String domainName = null;
        InetAddress addr = null;

        try {
            addr = InetAddress.getLocalHost();
            if (DEBUG) {
                System.err.println("getLocalHost -> " + addr);
            }
            InetAddress[] nameAddr = InetAddress.getAllByName(addr
                    .getHostAddress());

            for (int i = 0; i < nameAddr.length; i++) {
                if (DEBUG) {
                    System.err.println("getByName(getHostaddress)[" + i
                            + "] -> " + nameAddr[i]);
                }
                hostName = nameAddr[i].getHostName();
                if (DEBUG) {
                    System.err.println("getHostName(--) -> " + hostName);
                }
                domainName = domainName(hostName);
                if (domainName != null) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new BuildException("Could not find my host name" + e);
        }

        if (domainName == null && addr != null) {
            /* It seems our reverse lookup has failed miserably.
             * Maybe the direct lookup can help a bit... */
            String unchecked = addr.toString();
            int slash = unchecked.indexOf('/');
            if (slash > 0) {
                hostName = unchecked.substring(0, slash);
                domainName = domainName(hostName);
            }
        }

        if (hostName == null) {
            hostName = "(localhost)";
        }
        if (domainName == null) {
            domainName = "(none)";
        }

        addProperty("hostname", hostName);
        addProperty("domainname", domainName);

        domainname = domainName;
        hostname = hostName;
    }

    private void addProperty(String name, String value) {
        if (getProject().getUserProperty(name) == null) {
            getProject().setInheritedProperty(name, value);
        } else {
            log("Override ignored for " + name, Project.MSG_VERBOSE);
        }
    }

    public void setHostname(String name) {
        this.hostname = name;
    }

    public void setDomainname(String name) {
        this.domainname = name;
    }

}
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.net.InetAddress;

public class HostName extends Task {

    private String hostname;		// What is this used for?
    private String domainname;		// What is this used for?


    public void execute() throws BuildException {
	String hostName;
	String domainName;

	try {
	    InetAddress addr     = InetAddress.getLocalHost();
	    InetAddress nameAddr = InetAddress.getByName(addr.getHostAddress());
	    hostName             = nameAddr.getHostName();

	} catch (Exception e) {
	    throw new BuildException("Could not find my host name" + e);
	}

	int dot = hostName.indexOf('.');
	if (dot == -1) {
	    domainName = "";
	} else {
	    domainName = hostName.substring(dot + 1);
	}

	// System.err.println("hostname " + hostName);
	// System.err.println("domainname " + domainName);

	addProperty("hostname", hostName);
	addProperty("domainname", domainName);
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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class MapLibraryName extends Task {

    private String property;
    private String name;

    public void execute() throws BuildException {
	if (property == null) {
	    throw new BuildException("MapLibraryName requires a \"property\" field");
	}
	if (name == null) {
	    throw new BuildException("MapLibraryName requires a \"name\" field");
	}

	String libName = System.mapLibraryName(name);
	addProperty(property, libName);
    }

    private void addProperty(String name, String value) {
	if (getProject().getUserProperty(name) == null) {
	    getProject().setInheritedProperty(name, value);
	} else {
	    log("Override ignored for " + name, Project.MSG_VERBOSE);
	}
    }

    public void setProperty(String property) {
	this.property = property;
    }

    public void setName(String name) {
	this.name = name;
    }
}

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;


class OutputTypeEnum extends EnumeratedAttribute {

    private final static String[] values = new String[] {
	"shared",
	"static"
    };

    public OutputTypeEnum() {
	setValue("shared");
    }

    public String[] getValues() {
	return (String[]) values.clone();
    }

}


public class MapLibraryName extends Task {

    private String property;
    private String name;

    private OutputTypeEnum outtype = new OutputTypeEnum();


    public void execute() throws BuildException {
	if (property == null) {
	    throw new BuildException("MapLibraryName requires a \"property\" field");
	}
	if (name == null) {
	    throw new BuildException("MapLibraryName requires a \"name\" field");
	}

	String dirname = null;
	String basename = name;
	int slash = name.lastIndexOf(java.io.File.separatorChar);
	if (slash != -1) {
	    dirname = name.substring(0, slash);
	    basename = name.substring(slash + 1);
	}
	String libName = basename;
	if (outtype.getValue().equals("shared")) {
	    libName = System.mapLibraryName(basename);
	}
	if (dirname != null) {
	    libName = dirname + java.io.File.separator + libName;
	}
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

    public void setOuttype(OutputTypeEnum outputType) {
	this.outtype = outputType;
    }
}

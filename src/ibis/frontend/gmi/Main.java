package ibis.frontend.group;

import ibis.util.Analyzer;

import java.util.Vector;
import java.util.StringTokenizer;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;

import ibis.group.GroupInterface;

class Main { 
    
    public static String getFileName(String name, String pre) { 		
	return (pre + name + ".java");
    } 

    public static PrintWriter createFile(String name) throws Exception {

	File f = new File(name);
		
	if (!f.createNewFile()) { 
	    System.err.println("File " + name + " already exists!");
	    System.exit(1);
	}
	
	FileOutputStream fileOut = new FileOutputStream(f);
	
	return new PrintWriter(fileOut);
    } 

    public static void main(String [] args) { 
           
	Vector classes = new Vector();
	boolean verbose = false;
	Class groupInterface = null;

	if (args.length == 0) { 
	    System.err.println("Usage : java Main [-v] classname");
	    System.exit(1);
	}

	try { 
	    groupInterface = Class.forName("ibis.group.GroupInterface");
	} catch (Exception e) { 
	    System.err.println("Class ibis.group.GroupInterface not found");
	    System.exit(1);
	}

	for (int i=0;i<args.length;i++) { 
	    if (args[i].equals("-v")) { 
		verbose = true;
	    } else { 
		try { 
		    Class c = Class.forName(args[i]);
		    classes.addElement(c);
		} catch (Exception e) { 
		    System.err.println("Class " + args[i] + " not found");
		    System.exit(1);
		}
	    }
	} 
		
	for (int i=0;i<classes.size();i++) { 
	    
	    try { 
		PrintWriter output;
		Class subject = (Class) classes.get(i);
		
		if (verbose) { 
		    System.out.println("Handling " + subject.getName());
		}
		
		Analyzer a = new Analyzer(subject, groupInterface, verbose);
		a.start();

		if (subject.isInterface()) { 
		    output = createFile(getFileName(a.classname, "group_stub_"));			
		    new GMIStubGenerator(a, output, verbose).generate();
		    output.flush();

		    output = createFile(getFileName(a.classname, "group_parametervector_"));
		    new GMIParameterVectorGenerator(a, output, verbose).generate();
		    output.flush();
		} else {
		    output = createFile(getFileName(a.classname, "group_skeleton_"));			
		    new GMISkeletonGenerator(a, output, verbose).generate();
		    output.flush();
		}

	    } catch (Exception e) { 
		System.err.println("Main got exception " + e);
		e.printStackTrace();
		System.exit(1);
	    }
	} 
    } 
} 

package ibis.frontend.ibis;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

class Ibisc {
    JavaClass c;
    boolean verbose;
    boolean satinVerbose;
    boolean satinVerify;
    boolean iogenVerbose;
    boolean iogenVerify;
    boolean keep;
    boolean print;
    boolean invocationRecordCache;
    String className;
    String compiler;
    String mantac;
    boolean link;
    boolean doManta;
    boolean supportAborts;
    boolean inletOpt;
    boolean spawnCounterOpt;
    boolean local;
    IbiscFactory factory;
    Vector targets;
    Vector satinized = new Vector();
    String packageName;
    String exeName;

    Ibisc(boolean local, boolean verbose, 
	  boolean satinVerbose, boolean satinVerify,
	  boolean iogenVerbose, boolean iogenVerify,
	  boolean keep, boolean print, boolean invocationRecordCache, Vector targets, 
          String packageName, String exeName, String compiler, String mantac, 
          boolean link, boolean doManta, boolean supportAborts, boolean inletOpt, boolean spawnCounterOpt) {
	this.local = local;
	this.verbose = verbose;
	this.satinVerbose = satinVerbose;
	this.satinVerify = satinVerify;
	this.iogenVerbose = iogenVerbose;
	this.iogenVerify = iogenVerify;
	this.keep = keep;
	this.print = print;
	this.invocationRecordCache = invocationRecordCache;
	this.targets = targets;
	this.compiler = compiler;
	this.mantac = mantac;
	this.link = link;
	this.doManta = doManta;
	this.supportAborts = supportAborts;
	this.inletOpt = inletOpt;
	this.spawnCounterOpt = spawnCounterOpt;
	this.packageName = packageName;
	this.exeName = exeName;

	factory = new IbiscFactory();
	Repository.registerObserver(factory);
    }

    void compile(String target) {
	try {
	    String command = compiler + " " + target;
	    if (verbose) {
		System.out.println("Running: " + command);
	    }
	
	    Runtime r = Runtime.getRuntime();
	    Process p = r.exec(command);
	    java.io.BufferedInputStream stdout = new java.io.BufferedInputStream(p.getInputStream());
	    java.io.BufferedInputStream stderr = new java.io.BufferedInputStream(p.getErrorStream());
	    int res = p.waitFor();
	    if (res != 0) {
		System.err.println("Error compiling code (" + target + ").");
		System.err.println("Standard output:");
		while (true) {
		    try {
			int c = stdout.read();
			if (c == -1) {
			    break;
			}
			System.err.print((char)c);
		    } catch (IOException e) {
			break;
		    }
		}
		System.err.println("Error output:");
		while (true) {
		    try {
			int c = stderr.read();
			if (c == -1) {
			    break;
			}
			System.err.print((char)c);
		    } catch (IOException e) {
			break;
		    }
		}
		System.exit(1);
	    }
	    if (verbose) {
		System.out.println(" Done");
	    }
	} catch (Exception e) {
	    System.err.println("IO error: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    void mantaCompile(String target, boolean link) {
	try {
	    if (link) {
		System.out.println("Temporarily skip link phase");
		System.out.println(mantac + " -o " + exeName + " " + target + ".class and all other class files");
		return;
	    }
	    String command = mantac + (link ? " " : " -c ") + 
		(! link || exeName == null ? " " : "-o " + exeName + " ") +
		target + ".class";

	    if (verbose) {
		System.out.println("Running: " + command);
	    }
	
	    Runtime r = Runtime.getRuntime();
	    Process p = r.exec(command);
	    int res = p.waitFor();
	    if (res != 0) {
		System.err.println("Error compiling code (" + target + ").");
		System.exit(1);
	    }
	    if (verbose) {
		System.out.println(" Done");
	    }
	} catch (Exception e) {
	    System.err.println("IO error: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    boolean fileExists(String s) {
	File f = new File(s);
	return f.exists();
    }

    boolean fileNewer(String file1, String file2) {
	File f1 = new File(file1);
	File f2 = new File(file2);
	return f1.lastModified() > f2.lastModified();
    }
    
    public void start() {
	if (verbose) {
	    System.err.println("target: ");
	}
	for(int i=0; i<targets.size(); i++) {
	    if (verbose) {
		System.err.println(" " + targets.get(i));
	    }
	    doWorkForFile((String)targets.get(i));
	}
    }

    public void doWorkForFile(String javaFile) {
	if (!javaFile.endsWith(".java")) {
	    javaFile = javaFile + ".java";
	}

	if (!fileExists(javaFile)) {
	    System.err.println("File " + javaFile + " does not exist.");
	    System.exit(1);
	}

	String classFile = javaFile.substring(0, javaFile.length() - 5) + ".class";

	if (!fileExists(classFile) || fileNewer(javaFile, classFile)) {
	    compile(javaFile);
	} else {
	    if (verbose) {
		System.err.println("no need to compile " + javaFile);
	    }
	}

	// We should have bytecode now.
	className = javaFile.substring(0, javaFile.length() - 5);
	if (verbose) {
	    System.out.println("className = " + className);
	}

	if (packageName.equals("")) {
	    c = Repository.lookupClass(className);
	} else {
	    c = Repository.lookupClass(packageName + "." + className);
	}

	// Run satinc over all loaded classes
	for (int i=0; i<factory.getList().size(); i++) {
	    if (satinized.contains(factory.getList().get(i)) || 
	       ((String)factory.getList().get(i)).startsWith("Satin_")) {
		if (verbose) {
		    System.out.println("no need to run satinc on " + factory.getList().get(i));
		}
	    } else {
		if (verbose) {
		    System.out.println("running satinc on " + factory.getList().get(i));
		}
		new ibis.frontend.satin.Satinc(satinVerbose, local, satinVerify, keep, print, invocationRecordCache, 
					       (String)factory.getList().get(i), className, compiler, 
					       supportAborts, inletOpt, spawnCounterOpt).start();
		if (verbose) {
		    System.out.println(" Done");
		}
		
		satinized.add(factory.getList().get(i));
	    }
	}

	// Now generate serialization code for all classes, including the classes generated by satinc.
	if (verbose) {
	    System.out.println("running io generator on all files");
	}

	new ibis.frontend.io.IOGenerator(iogenVerbose, local, false, false, iogenVerify, null).scanClass(factory.getList());
	
	if (verbose) {
	    System.out.println(" Done");
	}

	// run manta's bytecode compiler on all generated code.
	// compile main class last.
	if (doManta) {
	    for (int i=0; i<factory.getList().size(); i++) {
		if (!((String)factory.getList().get(i)).equals(className)) {
		    mantaCompile((String)factory.getList().get(i), false);
		}
	    }

	    // and now main...
	    mantaCompile(className, link);
	}

//		for (int i=0; i<factory.getList().size(); i++) {
//			System.out.println("loadlist: " + factory.getList().get(i));
//		}
    }

    public static void usage() {
	System.err.println("Usage : ibisc [-v] [-sv] [-iv] [-keep] [-print] [-irc-off] " +
		   "[-compiler \"your compile command\" ] [-mantac \"your compile command\" ] " +
		   "[-c] [-o outfile] [-manta] [-no-aborts] [-no-inlet-opt] [-no-sc-opt] [-package] <java file(s)>");
	System.exit(1);
    }

    public static void main(String[] args) {
	boolean verbose = false;
	boolean keep = false;
	boolean print = false;
	boolean invocationRecordCache = true;
	boolean supportAborts = true;
	String compiler = "javac";
	String mantac = "mantac";
	boolean doManta = false;
	boolean inletOpt = true;
	boolean spawnCounterOpt = true;
	boolean satinVerbose = false;
	boolean satinVerify = false;
	boolean iogenVerbose = false;
	boolean iogenVerify = false;
	boolean link = true;
	Vector targets = new Vector();
	String packageName = "";
	String exeName = null;
	boolean local = true;

	for(int i=0; i<args.length; i++) {
	    if (args[i].equals("-v")) {
		verbose = true;
	    } else if (args[i].equals("-sv")) {
		satinVerbose = true;
	    } else if (args[i].equals("-sverify")) {
		satinVerify = true;
	    } else if (args[i].equals("-iv")) {
		iogenVerbose = true;
	    } else if (args[i].equals("-iverify")) {
		iogenVerify = true;
	    } else if (!args[i].startsWith("-")) {
		targets.add(args[i]);
	    } else if (args[i].equals("-package")) {
		packageName = args[i+1];
		i++;
	    } else if (args[i].equals("-compiler")) {
		compiler = args[i+1];
		i++;
	    } else if (args[i].equals("-mantac")) {
		mantac = args[i+1];
		i++;
	    } else if (args[i].equals("-keep")) {
		keep = true;
	    } else if (args[i].equals("-dir")) {
		local = false;
	    } else if (args[i].equals("-local")) {
		local = true;
	    } else if (args[i].equals("-o")) {
		exeName = args[i+1];
		i++;
	    } else if (args[i].equals("-manta")) {
		doManta = true;
	    } else if (args[i].equals("-c")) {
		link = false;
	    } else if (args[i].equals("-print")) {
		print = true;
	    } else if (args[i].equals("-irc-off")) {
		invocationRecordCache = false;
	    } else if (args[i].equals("-no-aborts")) {
		supportAborts = false;
	    } else if (args[i].equals("-no-inlet-opt")) {
		inletOpt = false;
	    } else if (args[i].equals("-no-sc-opt")) {
		spawnCounterOpt = false;
	    } else {
		usage();
	    }
	}

	if (targets.size() == 0) {
	    usage();
	}
	
	new Ibisc(local, verbose, satinVerbose, satinVerify, iogenVerbose, iogenVerify, keep, print, invocationRecordCache, 
	          targets, packageName, exeName, compiler, mantac, link, doManta, supportAborts, inletOpt, spawnCounterOpt).start();
    }
}

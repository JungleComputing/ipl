package ibis.frontend.rmi;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;

import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;

import ibis.util.BT_Analyzer;

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
		JavaClass rmiInterface = null;
	
		if (args.length == 0) { 
			System.err.println("Usage : java Main [-v] classname");
			System.exit(1);
		}

		int num = args.length;
		int i = 0;

		while (i<num) { 
			if (args[i].equals("-v")) { 
				verbose = true;
				args[i] = args[num-1];
				num--;
			} else { 
				i++;
			}
		} 

		rmiInterface = Repository.lookupClass("java.rmi.Remote");
		if (rmiInterface == null) {
			System.err.println("Class java.rmi.Remote not found");
			System.exit(1);
		}

		for (i=0;i<num;i++) { 
			JavaClass c = Repository.lookupClass(args[i]);
			if (c == null) {
				System.err.println("Class " + args[i] + " not found");
				System.exit(1);
			}
			classes.addElement(c);
		} 
				
		for (i=0;i<classes.size();i++) { 
			
			try { 
				PrintWriter output;
				JavaClass subject = (JavaClass) classes.get(i);
				
				if (verbose) { 
					System.out.println("Handling " + subject.getClassName());
				}
				
				BT_Analyzer a = new BT_Analyzer(subject, rmiInterface, verbose);
				a.start();

				output = createFile(getFileName(a.classname, "rmi_stub_"));			
				new RMIStubGenerator(a, output, verbose).generate();
				output.flush();

				output = createFile(getFileName(a.classname, "rmi_skeleton_"));			
				new RMISkeletonGenerator(a, output, verbose).generate();
				output.flush();

			} catch (Exception e) { 
				System.err.println("Main got exception " + e);
				e.printStackTrace();
				System.exit(1);
			}

		} 
	} 
} 



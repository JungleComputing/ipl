package ibis.frontend.repmi;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;

import java.util.StringTokenizer;
import java.util.Vector;

import ibis.util.BT_Analyzer;

import com.ibm.jikesbt.*;   

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
		BT_Class repmiInterface = null;
	
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

		BT_Factory.factory = new MyFactory(args, num, verbose);

		try { 
			repmiInterface = BT_Class.forName("manta.repmi.ReplicatedMethods");
		} catch (RuntimeException e) { 
			System.err.println("Class manta.repmi.ReplicatedMethods not found");
			System.exit(1);
		}

		for (i=0;i<num;i++) { 
			try { 
				BT_Class c = BT_Class.forName(args[i]);
				classes.addElement(c);
			} catch (Exception e) { 
				System.err.println("Class " + args[i] + " not found");
				System.exit(1);
			}
		} 
				
		for (i=0;i<classes.size();i++) { 
			
			try { 
				PrintWriter output;
				BT_Class subject = (BT_Class) classes.get(i);
				
				if (verbose) { 
					System.out.println("Handling " + subject.getName());
				}
				
				BT_Analyzer a = new BT_Analyzer(subject, repmiInterface, verbose);
				a.start();

				output = createFile(getFileName(a.classname, "repmi_stub_"));			
				new RepMIStubGenerator(a, output, verbose).generate();
				output.flush();

				output = createFile(getFileName(a.classname, "repmi_skeleton_"));			
				new RepMISkeletonGenerator(a, output, verbose).generate();
				output.flush();

			} catch (Exception e) { 
				System.err.println("Main got exception " + e);
				e.printStackTrace();
				System.exit(1);
			}

		} 
	} 
} 

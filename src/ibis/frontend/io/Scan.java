package ibis.frontend.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class Failed { 
    String [] class_names;
    StringBuffer error;
    int exit;
}

public class Scan { 

    public static final int BATCH_SIZE = 20;

    private static Vector classes = new Vector();
    private static Vector failed = new Vector();
    private static Runtime r = Runtime.getRuntime();

    private static String [] cpath = getClassPath();

    public static String [] getClassPath() {
	int i=0;

	String cp = System.getProperty("java.class.path");
	StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
	String[] entries = new String[st.countTokens()];

	while(st.hasMoreTokens()) {
	    entries[i++] = st.nextToken();
	}
	return entries;
    }

    public static String path2qualified(String name) { 
	
	// remove the '.class' part
	if (name.endsWith(".class")) { 			
	    name = name.substring(0,name.length()-6);
	} 

	name = name.replace('/', '.');
	return name;
    } 

    public static void handleClasses(String [] class_names) { 
	int result = 0;
	InputStream i1 = null, i2 = null;

	try { 	
	    String command = "java ibis.frontend.io.IOGenerator ";

	    for (int i=0;i<BATCH_SIZE;i++) { 
		if (class_names[i] != null) { 
		    command += class_names[i] + " ";
		}
	    }

//	    System.out.println("Command = " + command);

	    Process p = r.exec(command);
	    i1 = p.getInputStream();
	    i2 = p.getErrorStream();
	    result = p.waitFor();
	} catch (Exception e) { 
	    result = -1;
	} 
	
	if (result != 0) { 
	    Failed f = new Failed();

	    f.class_names = class_names;
	    f.error = new StringBuffer("");					
	    f.exit = result;

	    f.error.append("stdout:\n");
	    f.error.append("      : ");
		
	    int data = 0;

	    try { 
		do { 
		    data = i1.read();
		    if (data != -1) { 
			f.error.append(((char) data));
			if (data == '\n') { 
			    f.error.append("      : ");
			} 
		    }
		} while (data != -1);

	    } catch (IOException e) { 
		// ignore.
	    } 

	    f.error.append("\nstderr: \n");
	    f.error.append("      : ");

	    try { 
		do { 
		    data = i2.read();
		    if (data != -1) { 
			f.error.append(((char) data));
			if (data == '\n') { 
			    f.error.append("      : ");
			} 
		    }
		} while (data != -1);

	    } catch (IOException e) { 
		// ignore.
	    } 

	    failed.add(f);
	} 
    } 

    public static void scanJar(String jarfile) throws IOException {
	JarFile jarf = new JarFile(jarfile);
	Enumeration e = jarf.entries();
	    
	while (e.hasMoreElements()) { 
	    JarEntry entry = (JarEntry) e.nextElement();
		    
	    if (!entry.isDirectory() && entry.getName().endsWith(".class")) { 				
		classes.add(path2qualified(entry.getName()));	
	    }
	}

	jarf.close();
	System.out.println("Jarfile " + jarfile + " contains " + classes.size() + " classes.");
	    
	System.out.println("Starting rewrite");
	    
	long start = System.currentTimeMillis();
	int total = classes.size();
	String [] batch = new String[BATCH_SIZE];
	    
	for (int i=0;i<total;i+=BATCH_SIZE) { 
	    for (int j=0;j<BATCH_SIZE;j++) { 		
		if (i+j < total) {
		    batch[j] = (String) classes.get(i+j);
		    System.out.println(batch[j] + " (" + (i+j) + " of " + total + ")");
		} else { 
		    batch[j] = null;
		}
	    }

	    long temp1 = System.currentTimeMillis();

	    handleClasses(batch);			

	    long temp2 = System.currentTimeMillis();
		    
	    System.out.println("Handled in " + ((temp2-temp1)/1000.0) + " seconds.");
	} 
	    
	long end = System.currentTimeMillis();
	    
	System.out.println("Done rewrite in " + ((end-start)/1000.0) + " seconds.");
	    
	total = failed.size();
	if (total > 0) { 
	    System.out.println("The following batch failed to rewrite properly:");
		
	    for (int i=0;i<total;i++) { 
		Failed f = (Failed) failed.get(i);
		
		for (int j=0;j<BATCH_SIZE;j++) { 
		    System.out.println(f.class_names[j]);
		}
			    
		System.out.println("errorcode " + f.exit + "\n" + f.error.toString());				
	    } 		
	}
    }

    public static void main(String [] args) { 
	try { 	
	    if (args[0].endsWith(".jar")) { 
		scanJar(args[0]);
	    }
	} catch (Exception e) { 
	    e.printStackTrace();
	}
    } 
} 

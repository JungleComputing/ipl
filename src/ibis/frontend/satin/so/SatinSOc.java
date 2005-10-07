
package ibis.frontend.satin.so;

import ibis.frontend.generic.BT_Analyzer;
import ibis.util.RunProcess;

import java.io.*;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/** The Satin Shared Objects compiler and bytecode rewriter.
 *  This class creates SOInvocationRecords for write methods
 *  of shared objects. It also rewrite the bytecode of each 
 *  write method and inserts communication code
 */

class SatinSOc {

    static ClassGen classGen;
    static InstructionFactory insFactory;
    static ConstantPoolGen cpg;

    static boolean verbose = true;
    static boolean keep = true;
    static Object compiler = null;
    static boolean local = true;

    // @TODO: add a $rewritten$ field, so that we don't rewrite the 
    // class twice

    public static void main(String[] args) {

        Vector classes = new Vector();
	Vector classList = new Vector();

        String javadir = System.getProperty("java.home");
        String javapath = System.getProperty("java.class.path");
        String filesep = System.getProperty("file.separator");
        String pathsep = System.getProperty("path.separator");

        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                classList.add(args[i]);
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-verbose")) {
                verbose = true;
            } else if (args[i].equals("-no-verbose")) {
                verbose = false;
            } else if (args[i].equals("-compiler")) {
                compiler = args[i + 1];
                i++;
            } else if (args[i].equals("-javahome")) {
                javadir = args[i + 1];
                i++;
            } else if (args[i].equals("-keep")) {
                keep = true;
            } else if (args[i].equals("-no-keep")) {
                keep = false;
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else {
                usage();
            }
        }

        if (classList.size() == 0) {
            usage();
        }

        if (compiler == null) {
            String[] cmd = new String[] {
                    javadir + filesep + "bin" + filesep + "javac", "-g",
                    "-classpath", javapath + pathsep };
            compiler = cmd;
        }


        JavaClass writeMethodsInterface = 
	    Repository.lookupClass("ibis.satin.so.WriteMethodsInterface");

        if (writeMethodsInterface == null) {
            System.err.println("Class WriteMethodInterface not found");
            System.exit(1);
        }

	//lookup classes from the argument list
        for (int i = 0; i < classList.size(); i++) {
            JavaClass c = Repository.lookupClass((String)classList.get(i));
            if (c == null) {
                System.err.println("Class " + ((String)classList.get(i)) + " not found");
                System.exit(1);
            }
            classes.addElement(c);
        }

	//for each class in the argument list..
        for (int i = 0; i < classes.size(); i++) {

            try {

                PrintWriter output;
                JavaClass subjectClass = (JavaClass) classes.get(i);

 		classGen = new ClassGen(subjectClass);
		insFactory = new InstructionFactory(classGen);
		cpg = classGen.getConstantPool();

                if (subjectClass.isInterface()) {
                    continue;
                }

		//check if it derives from the ibis.satin.so.SharedObject class
		if (!Repository.instanceOf(subjectClass, 
					   "ibis.satin.so.SharedObject")) {
		    continue;
		}

                if (verbose) {
                    System.out.println("Handling " 
				       + subjectClass.getClassName());
                }


		if (classGen.containsField("$SOrewritten$")!=null) {
		    System.err.println("Class " + subjectClass.getClassName() 
				       + " is already rewritten");
		    continue;
		}
		
		BT_Analyzer a = new BT_Analyzer(subjectClass, 
						writeMethodsInterface, verbose);
		a.start(false);


		//		Vector methods = a.subjectSpecialMethods;
		Method[] methods = subjectClass.getMethods();

		if (methods == null) {
		    continue;
		}
		    

		//add the $SOrewritten$ field
		classGen.addField(new FieldGen(Constants.ACC_STATIC, Type.BOOLEAN,
					    "$SOrewritten$", cpg).getField());		

		//add things to the constant pool
		/*		int getSatin = cpg.addMethodref("ibis.satin.impl.Satin",
				       "getSatin",
				       "()Libis/satin/impl/Satin");
		int broadcastSOInvocation = 
		    cpg.addMethodref("ibis.satin.impl.Satin",
				     "broadcastSOInvocation",
				     "(Libis/satin/impl/SOInvocationRecord)V");*/
	    
		//rewrite methods
		for (int j = 0; j < methods.length; j ++) {		    
		    Method method = methods[j];		    
		    //		    System.err.println(method.toString());
		    if (a.isSpecial(method)) {
			//change the name of the method to so_local_methodName
			//create a new methodName with the following body
			//Satin.getSatin().broadcastSOInvocation(new SOInvRecord())
			//return so_local_methodName
			rewriteMethod(method, subjectClass.getClassName());


		    }
		    /*	    if(method.getName().equals("<init>")) {
			System.err.println("rewriting constructor");
			rewriteConstructor(method, subjectClass.getClassName());
			}*/

		}

		JavaClass newSubjectClass = classGen.getJavaClass();
		Repository.removeClass(subjectClass);
		Repository.addClass(newSubjectClass);
		//dump the class
                String clnam = newSubjectClass.getClassName();
                String dst;
                if (local) {
                    dst = clnam.substring(clnam.lastIndexOf('.')+1);
                } else {
                    dst = clnam.replace('.', java.io.File.separatorChar);
                }
                dst = dst + ".class";
		newSubjectClass.dump(dst);

		//generate so invocation records
		for (int j = 0; j < methods.length; j ++) {
		    Method method = methods[j];
		    if (a.isSpecial(method)) {
			//generate an SOInvocationRecord for this method
			writeInvocationRecord(method, subjectClass.getClassName());

			compileGenerated(invocationRecordName(method,
							      subjectClass.
							      getClassName()));		 
			if (!keep) { // remove generated files 
			    removeFile(invocationRecordName(method, 
							    subjectClass.getClassName())
				       + ".java");
			}
		    }
		}

            } catch (Exception e) {
                System.err.println("Got exception during processing of "
                        + ((JavaClass) classes.get(i)).getClassName());
                System.err.println("exception is: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

    public static void usage() {
        System.err.println("Usage : java SatinSOc [[-no]-verbose] [[-no]-keep] "
			   + "[-javahome \"your java home\" ] "
			   + "[-compiler \"your compile command\" ] "
			   + "[-dir|-local] "
			   + "<classname>*");
        System.exit(1);
    }

    private static void writeInvocationRecord(Method m, String clname) 
	    throws java.io.IOException {

	    int i;
	    String name = invocationRecordName(m, clname);

	    FileOutputStream f = new FileOutputStream(name + ".java");
	    BufferedOutputStream b = new BufferedOutputStream(f);
	    DollarFilter b2 = new DollarFilter(b);
	    PrintStream out = new PrintStream(b2);

	    System.err.println("Generating inv rec for method: " + m.getName()
			       + " with signature: " + m.getSignature());

	    /* Copied from MethodTable.java; I have no clue why
	       m.getArgumentTypes is not used*/
	    Type[] params = Type.getArgumentTypes(m.getSignature());
	    String[] params_types_as_names = new String[params.length];

	    for (i = 0; i < params.length; i++ ) {
		if (params[i] instanceof ObjectType) {
		    String clnam = ((ObjectType) params[i]).getClassName();
		    if (!Repository.implementationOf(clnam, 
						     "java.io.Serializable")) {
			System.err.println(clname
					   + ": write method"
					   + " with non-serializable parameter type "
					   + clnam);
			System.err.println(clname
					   + ": all parameters of a write method"
					   + " must be serializable.");
			System.exit(1);
		    }
		}
		params_types_as_names[i] = params[i].toString();
	    }

	    out.println("import ibis.satin.so.*;\n");
            out.println("import ibis.satin.impl.*;\n");
	    out.println("public final class " + name 
			+ " extends SOInvocationRecord {");

	    //fields
	    for (i = 0; i < params_types_as_names.length; i++ ) {
		out.println("\t" + params_types_as_names[i] + " param" 
			    + i + ";");
	    }
	    out.println();

	    //constructor
	    out.print("\tpublic " + name + "(String objectId, ");
	    for (i = 0; i < params_types_as_names.length-1; i++) {
		out.print(params_types_as_names[i] + " param" + i + ", ");
	    }
	    out.println(params_types_as_names[i] + " param" + i + ") {");
	    out.println("\t\tsuper(objectId);");
	    for (i = 0; i < params_types_as_names.length; i++) {
		//		if (params[i] instanceof BasicType) {
		    out.println("\t\tthis.param" + i + " = param" + i + ";");
		    /*		} else {
		    //copy the parameter
		    out.println("\t\tthis.param" + i + " = (" + params_types_as_names[i]
				+ ") cloneObject(param" + i + ");");
				}*/
	    }
	    out.println("\t}\n");

	    //invoke method
	    out.println("\tpublic void invoke(SharedObject object) {");
	    out.println("\t\t" + clname + " obj = (" + clname + ") object;");
	    out.println("\t\ttry{");
	    out.print("\t\t\tobj.so_local_" + m.getName() + "(");
	    for (i = 0; i < params_types_as_names.length-1; i++ ) {
		out.print("param" + i + ", ");
	    }
	    out.println("param" + i + ");");
	    out.println("\t\t} catch (Throwable t) {");
	    out.println("\t\t\t/* exceptions will be only thrown at the originating node*/");
	    out.println("\t\t}");
	    out.println("\t}");
	    out.println();
	    out.println("}");
	    out.close();
	}

    static String invocationRecordName(Method m, String clnam) {
        return ("Satin_" + clnam + "_" 
		+ do_mangle(m.getName(), m.getSignature())
                + "_SOInvocationRecord").replace('.', '_');
    }

    static void compileGenerated(String className) {
        try {
            RunProcess p;
            if (compiler instanceof String) {
                String command = (String) compiler + " " + className + ".java";
                if (verbose) {
                    System.out.println("Running: " + command);
                }

                p = new RunProcess(command);
            } else {
                String[] comp = (String[]) compiler;
                String[] cmd = new String[(comp.length + 1)];
                for (int i = 0; i < comp.length; i++) {
                    cmd[i] = comp[i];
                }
                cmd[comp.length] = className + ".java";

                if (verbose) {
                    System.out.print("Running: ");
                    for (int i = 0; i < cmd.length; i++) {
                        System.out.print(cmd[i] + " ");
                    }
                    System.out.println("");
                }
                p = new RunProcess(cmd, new String[0]);
            }
            int res = p.getExitStatus();
            if (res != 0) {
                System.err.println("Error compiling generated code ("
                        + className + ").");
                byte[] err = p.getStderr();
                System.err.write(err, 0, err.length);
                System.err.println("");
                System.exit(1);
            }
            if (verbose) {
                System.out.println("Done");
            }
            Repository.lookupClass(className);
        } catch (Exception e) {
            System.err.println("IO error: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void removeFile(String name) {
        if (verbose) {
            System.out.println("removing " + name);
        }

        try {
            File f = new File(name);
            f.delete();
        } catch (Exception e) {
            System.err.println("Warning: could not remove " + name);
        }
    }


   static void rewriteMethod(Method m, String clnam) {
       MethodGen origMethodGen;
       MethodGen newMethodGen;
       InstructionList newMethodInsList;
       Type[] arguments;
       Type[] objectId_and_arguments;
       Type returnType;
       int oldAccessFlags;
       int monitorVarAddr;

       System.err.println("Rewriting method: " + m.getName() 
			  + " with signature: " + m.getSignature());

       //prefix the original method name with so_local
       origMethodGen = new MethodGen(m, clnam, cpg);
       origMethodGen.setName("so_local_" + m.getName());
       origMethodGen.setMaxStack();
       origMethodGen.setMaxLocals();
       oldAccessFlags = origMethodGen.getAccessFlags();
       origMethodGen.setAccessFlags(0x0001); //public
       classGen.removeMethod(m);
       classGen.addMethod(origMethodGen.getMethod());
       
       //create the new method with the following body:
       //Satin.getSatin().
       //broadcastSOInvocation(new Satin_methodname_SOInvocationRecord(params))
       //synchronized(Satin.getSatin()) {
       //return so_local_methodname(params);
       //}

	newMethodInsList = new InstructionList();
	//broadcast 
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
						     "getSatin",
						     new ObjectType("ibis.satin.impl.Satin"),
						     new Type[] {},
						     Constants.INVOKESTATIC));
	newMethodInsList.append(insFactory.createNew(invocationRecordName(m, clnam)));
	newMethodInsList.append(insFactory.createDup(1));
	newMethodInsList.append(new ALOAD(0));
	newMethodInsList.append(insFactory.createGetField(clnam,
						       "objectId",
						       Type.STRING));
	arguments = m.getArgumentTypes();			     
	int k1 = 0;
	for (int k = 0; k < arguments.length; k++) {
	    newMethodInsList.append(insFactory.createLoad(arguments[k], k1+1));
	    k1 += arguments[k].getSize();
	}

	objectId_and_arguments = new Type[arguments.length+1];
	objectId_and_arguments[0] = Type.STRING;
	for (int k = 0; k < arguments.length; k++) {
	    objectId_and_arguments[k+1] = arguments[k];
	}

	newMethodInsList.append(insFactory.createInvoke(invocationRecordName(m, clnam), 
						   "<init>",
						   Type.VOID,
						   objectId_and_arguments,
						   Constants.INVOKESPECIAL));
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
						    "broadcastSOInvocation",
						    Type.VOID,
						    new Type[] 
	    {new ObjectType("ibis.satin.impl.SOInvocationRecord")},
						    Constants.INVOKEVIRTUAL));
	//enter the monitor	
	monitorVarAddr = 1;
	for (int k = 0; k < arguments.length; k++) {
	    monitorVarAddr += arguments[k].getSize();
	}
	newMethodInsList.append(insFactory.createInvoke("ibis.satin.impl.Satin",
							 "getSatin",
							 new ObjectType("ibis.satin.impl.Satin"),
							 new Type[] {},
							 Constants.INVOKESTATIC));
	newMethodInsList.append(insFactory.createDup(1));
	newMethodInsList.append(new ASTORE(monitorVarAddr));
	newMethodInsList.append(new MONITORENTER());
	//call the object method
	InstructionHandle from1 = newMethodInsList.append(new ALOAD(0));
	k1 = 0;
	for (int k = 0; k < arguments.length; k++) {
	    newMethodInsList.append(insFactory.createLoad(arguments[k], k1+1));
	    k1 += arguments[k].getSize();
	}
	returnType = m.getReturnType();
	newMethodInsList.append(insFactory.createInvoke(clnam,
						     "so_local_" + m.getName(),
						     returnType,
						     arguments,
						     Constants.INVOKEVIRTUAL));
	//exit the monitor
	newMethodInsList.append(new ALOAD(monitorVarAddr));
	newMethodInsList.append(new MONITOREXIT());
	//return statement
	InstructionHandle to1 = newMethodInsList.append(insFactory.createReturn(returnType));
	//exception handlers
	InstructionHandle from2 = newMethodInsList.append(new ASTORE(monitorVarAddr+1));
	newMethodInsList.append(new ALOAD(monitorVarAddr));
	newMethodInsList.append(new MONITOREXIT());
	InstructionHandle to2 = newMethodInsList.append(new ALOAD(monitorVarAddr+1));
	newMethodInsList.append(new ATHROW());

	newMethodGen = new MethodGen(oldAccessFlags,
				     returnType,
				     arguments,
				     origMethodGen.getArgumentNames(),
				     m.getName(),
				     clnam,
				     newMethodInsList,
				     cpg);
 
	newMethodGen.addExceptionHandler(from1, to1, from2, null);
	newMethodGen.addExceptionHandler(from2, to2, from2, null);
	newMethodGen.setMaxStack();
	newMethodGen.setMaxLocals();
		    
	classGen.addMethod(newMethodGen.getMethod());
	 	
    }

   static void rewriteConstructor(Method m, String clnam) {

       MethodGen methodGen = new MethodGen(m, clnam, cpg);
       InstructionList il = methodGen.getInstructionList();

       InstructionHandle ret_ih = il.getEnd();

       //add 'Satin.getSatin().broadcastSharedObject(this);' at the end
       //of the constructor

       il.insert(ret_ih, 
		 insFactory.createInvoke("ibis.satin.impl.Satin",
					 "getSatin",
					 new ObjectType("ibis.satin.impl.Satin"),
					 new Type[] {},
					 Constants.INVOKESTATIC));
       il.insert(ret_ih, new ALOAD(0));
       il.insert(ret_ih,
		 insFactory.createInvoke("ibis.satin.impl.Satin",
					 "broadcastSharedObject",
					 Type.VOID,
					 new Type[] {new ObjectType("ibis.satin.so.SharedObject")},
					 Constants.INVOKEVIRTUAL));

       System.err.println("il length: " + il.getLength());

       methodGen.setInstructionList(il);
       methodGen.setMaxStack();
       methodGen.setMaxLocals();
       classGen.removeMethod(m);
       classGen.addMethod(methodGen.getMethod());
       
   }

    

    static String do_mangle(StringBuffer s) {
        // OK, now sanitize parameters
        int i = 0;
        while (i < s.length()) {
            switch (s.charAt(i)) {
            case '$':
            case '.':
            case '/':
                s.setCharAt(i, '_');
                break;

            case '_':
                s.replace(i, i + 1, "_1");
                break;

            case ';':
                s.replace(i, i + 1, "_2");
                break;

            case '[':
                s.replace(i, i + 1, "_3");
                break;

            default:
                break;
            }
            i++;
        }
        return s.toString();
    }

    static String do_mangle(String name, String sig) {
        StringBuffer s = new StringBuffer(sig);
        name = do_mangle(new StringBuffer(name));

        int open = sig.indexOf("(");
        if (open == -1) {
            return name;
        }
        s.delete(0, open + 1);

        sig = s.toString();

        int close = sig.indexOf(")");
        if (close == -1) {
            return name;
        }
        s.delete(close, s.length());

        return name + "__" + do_mangle(s);
    }

    private static class DollarFilter extends FilterOutputStream {
	DollarFilter(OutputStream out) {
	    super(out);
	}

	public void write(int b) throws IOException {
	    if (b == '$') {
		super.write('.');
	    } else {
		super.write(b);
	    }
	}
    }

	    
}

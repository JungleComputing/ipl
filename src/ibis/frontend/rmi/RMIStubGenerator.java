package ibis.frontend.rmi;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;

import java.util.Vector;
import java.io.PrintWriter;
import ibis.util.BT_Analyzer;

class RMIStubGenerator extends RMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	RMIStubGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 

	void methodHeader(Method m, int number) { 
		
		Type ret          = getReturnType(m);
		Type[] params = getParameterTypes(m);
		
		output.print("\tpublic final " + ret + " " + m.getName() + "(");
		
		for (int j=0;j<params.length;j++) { 
			output.print(params[j] + " p" + j);

			if (j<params.length-1) { 
				output.print(", ");
			} 
		}
		
		output.print(") throws java.rmi.RemoteException {\n");
	}

	void methodBody(Method m, int number) { 
	    Type ret          = getReturnType(m);
	    Type[] params = getParameterTypes(m);
	    
	    //if (verbose) System.out.println(m.getName() + " is a " + (write ? "write" : "read") + " method");
	    
//	    output.println("\t\t\tif (RTS.DEBUG) System.out.println(\"rmi_stub_" + data.classname + "." + m.getName() + " doing RMI call\");");	

	    if (!ret.equals(Type.VOID)) { 
		output.println("\t\t" + getInitedLocal(ret, "result") + ";");
	    }

	    output.println("\t\ttry {");
	    output.println("\t\t\tWriteMessage w = send.newMessage();");
	    output.println("\t\t\tw.writeInt(" + number + ");");
	    output.println("\t\t\tw.writeInt(stubID);");

	    for (int j=0;j<params.length;j++) { 
		output.println(writeMessageType("\t\t\t", "w", params[j], "p" + j));
	    }
	    
	    output.println("\t\t\tw.send();");
	    output.println("\t\t\tw.finish();");

	    output.println("\t\t\tReadMessage r = reply.receive();");
	    output.println("\t\t\tif (r.readByte() == ibis.rmi.Protocol.EXCEPTION) {");
	    output.println("\t\t\t\tException e = (Exception) r.readObject();");
	    output.println("\t\t\t\tr.finish();");
	    output.println("\t\t\t\tthrow e;");
	    output.println("\t\t\t}");
	    output.println("\t\t\t//else: normal result");	    
	    if (!ret.equals(Type.VOID)) { 		
		output.println(readMessageType("\t\t\t", "result", "r", ret));
	    }
	    output.println("\t\t\tr.finish();");

	    output.println("\t\t} catch (Exception e) {");
	    output.println("\t\t\tthrow new RemoteException(\"oops\" + e);");
	    output.println("\t\t}");
	} 

	void methodTrailer(Method m) { 

	    Type ret          = getReturnType(m);

	    if (!ret.equals(Type.VOID)) {       
		output.println("\t\treturn result;"); 
	    } 
	    output.println("\t}\n");			
	} 

	void header() { 

		Vector interfaces = data.specialInterfaces;

		if (data.packagename != null) { 
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.rmi.*;");		
		output.println("import ibis.ipl.*;");			
		output.println("import java.rmi.RemoteException;");			
		output.println();

		output.print("public final class rmi_stub_" + data.classname + " extends ibis.rmi.Stub implements ");		

		for (int i=0;i<interfaces.size();i++) { 
			output.print(((JavaClass) interfaces.get(i)).getClassName());

			if (i<interfaces.size()-1) { 
				output.print(", ");
			}  
		}
			
		output.println(" {\n");
	} 

	void constructor() { 
		output.println("\tpublic rmi_stub_" + data.classname + "() {");
//		output.println("\t\tsuper();");
		output.println("\t}\n");
	} 

	void body(Vector methods) { 

		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			methodHeader(m, i);
			methodBody(m, i);
			methodTrailer(m);		
		} 
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 

	void generate() { 		
		header();		
		constructor();		
		body(data.subjectSpecialMethods);
		trailer();
	} 

} 


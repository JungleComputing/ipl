package ibis.frontend.rmi;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;

import java.util.Vector;
import java.io.PrintWriter;
import ibis.util.BT_Analyzer;

class RMISkeletonGenerator extends RMIGenerator { 

	BT_Analyzer data;
	PrintWriter output;
	boolean verbose;

	String dest_name;

	RMISkeletonGenerator(BT_Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 
     
	void header() { 
		if (data.packagename != null) {
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.rmi.*;");		
		output.println("import java.lang.reflect.*;");	
		output.println("import ibis.ipl.*;");			
		output.println();
	
		output.println("public final class rmi_skeleton_" + dest_name + " extends ibis.rmi.server.Skeleton implements ibis.ipl.Upcall {");
		output.println();		
	} 

	void messageHandler(Vector methods) { 

		output.println("\tpublic final void upcall(ReadMessage r) {");
		output.println();		

		output.println("\t\ttry {");
		
		//gosia
		output.println("\t\t\tRTS.setClientHost(r.origin().ibis().address());");		
		//end gosia

		output.println("\t\t\tException ex = null;");		
		output.println("\t\t\tint method = r.readInt();");		
		output.println("\t\t\tint stubID = r.readInt();");		
		output.println();		
		
		output.println("\t\t\tswitch(method) {");		

		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			Type ret = getReturnType(m);
			Type[] params = getParameterTypes(m);
			
			output.println("\t\t\tcase " + i + ":");
			output.println("\t\t\t{");		       

			output.println("\t\t\t\t/* First - Extract the parameters */");		       
			
			for (int j=0;j<params.length;j++) { 
				Type temp = params[j];
				output.println(readMessageType("\t\t\t\t", temp + " p" + j, "r", temp));
			}
			
			// we should try to optimize this finish away, to avoid thread cration!!! --Rob
			output.println("\t\t\t\tr.finish();");
			output.println();
			
			output.println("\t\t\t\t/* Second - Invoke the method */");		       
			
			if (!ret.equals(Type.VOID)) { 
				output.println("\t\t\t\t" + getInitedLocal(ret, "result") + ";");
			}

			output.println("\t\t\t\ttry {");
			output.print("\t\t\t\t\t");
		
			if (!ret.equals(Type.VOID)) { 
				output.print("result = ");
			} 

			output.print("((" + dest_name + ") destination)." + m.getName() + "(");
			
			for (int j=0;j<params.length;j++) { 
				output.print("p" + j);
				
				if (j<params.length-1) { 
					output.print(", ");
				}
			}
			output.println(");");	
			output.println("\t\t\t\t} catch (Exception e) {");
			output.println("\t\t\t\t\tex = e;");
			output.println("\t\t\t\t}");

			output.println("\t\t\t\tWriteMessage w = stubs[stubID].newMessage();");		

			output.println("\t\t\t\tif (ex != null) {");
			output.println("\t\t\t\t\tw.writeByte(ibis.rmi.Protocol.EXCEPTION);");
			output.println("\t\t\t\t\tw.writeObject(ex);");
			output.println("\t\t\t\t} else {");
			output.println("\t\t\t\t\tw.writeByte(ibis.rmi.Protocol.RESULT);");

			if (!ret.equals(Type.VOID)) { 
				output.println(writeMessageType("\t\t\t\t\t", "w", ret, "result"));
			} 

			output.println("\t\t\t\t}");
			output.println("\t\t\t\tw.send();");
			output.println("\t\t\t\tw.finish();");
					       
			output.println("\t\t\t\tbreak;");
			output.println("\t\t\t}");
			output.println();
		}

		output.println("\t\t\tcase -1:");
		output.println("\t\t\t{");		       	
		output.println("\t\t\t\t/* Special case for new stubs that are connecting */");		       		
		output.println("\t\t\t\tReceivePortIdentifier rpi = (ReceivePortIdentifier) r.readObject();");		       
		output.println("\t\t\t\tr.finish();");		       
		output.println("\t\t\t\tint id = addStub(rpi);");
		output.println("\t\t\t\tWriteMessage w = stubs[id].newMessage();");
		output.println("\t\t\t\tw.writeInt(id);");
		output.println("\t\t\t\tw.writeObject(stubType);");
		output.println("\t\t\t\tw.send();");
		output.println("\t\t\t\tw.finish();");
		output.println("\t\t\t\tbreak;");		       
		output.println("\t\t\t}");		       
		output.println();

		output.println("\t\t\tdefault:");
		output.println("\t\t\t\tSystem.err.println(\"OOPS: group_skeleton got illegal method number!\");");
		output.println("\t\t\t\tSystem.exit(1);");
		output.println("\t\t\t\tbreak;");
		
		output.println("\t\t\t}");
		output.println("\t\t} catch (IbisIOException ie) {");
		output.println("\t\t\tSystem.err.println(\"EEK: got exception in upcall \" + ie);");
		output.println("\t\t\tie.printStackTrace();");
		output.println("\t\t}");

		output.println("\t}");
		output.println();			       
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 
       
	void constructor(Vector methods) { 

		output.println("\tpublic rmi_skeleton_" + data.classname + "() {");

//		output.println("\t\tsuper();");
		output.println("\t\tstubType = \"rmi_stub_" + data.classname + "\";");
		output.println();
		output.println("\t}\n");
	} 

	void generate() { 

		dest_name = data.classname;

		header();		
		constructor(data.specialMethods);
		messageHandler(data.specialMethods);		
		trailer();
	} 
} 







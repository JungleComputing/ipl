package ibis.frontend.group;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.Analyzer;

class GMIStubGenerator extends GMIGenerator { 

	Analyzer data;
	PrintWriter output;
	boolean verbose;

	GMIStubGenerator(Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 


	void methodHeader(Method m) { 
		
		Class ret       = m.getReturnType();
		Class [] params = m.getParameterTypes();
	       
		output.print("\tpublic final " + getType(ret) + " " + m.getName() + "(");
		
		for (int j=0;j<params.length;j++) { 
			output.print(getType(params[j]) + " p" + j);

			if (j<params.length-1) { 
				output.print(", ");
			} 
		}
		
		output.print(") {\n");
			
		if (!ret.equals(Void.TYPE)) { 
			
			if (ret.isPrimitive()) {
				
				output.print("\t\t" + getType(ret) + " result = ");
				
				if (ret.equals(Byte.TYPE)) { 
					output.println("(byte) 0;");
				} else if (ret.equals(Character.TYPE)) { 
					output.println("(char) 0;");
				} else if (ret.equals(Short.TYPE)) {
					output.println("(short) 0;");
				} else if (ret.equals(Integer.TYPE)) {
					output.println("(int) 0;");
				} else if (ret.equals(Long.TYPE)) {
					output.println("(long) 0;");
				} else if (ret.equals(Float.TYPE)) {
					output.println("(float) 0.0;");
				} else if (ret.equals(Double.TYPE)) {
					output.println("(double) 0.0;");
				} else if (ret.equals(Boolean.TYPE)) {
					output.println("false;");
				} 					       
			} else { 
				output.println("\t\t" + getType(ret) + " result = null;");
			} 
		
		} 
	}

	void methodBody(Method m, int number) { 
		
		Class ret = m.getReturnType();
		Class [] params = m.getParameterTypes();

		output.println("\t\ttry {");			

		output.println("\t\t\tint ticket = 0;");			
		output.println("\t\t\tGroupMethod method = methods[" + number + "];");			

		output.println("\t\t\tswitch (method.invocationMode) {");			

		/* ======================================================================================================================= */
		output.println();			
		output.println("\t\t\tcase Group.LOCAL:");			
		output.println("\t\t\t{");			

		output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing LOCAL call\");");	

		output.print("\t\t\t\t");	

		if (!ret.equals(Void.TYPE)) { 
			output.print("result = ");			
		} 
		
		output.print("((group_skeleton_" + data.classname + ")localSkeleton).GMI_" + m.getName() + "(method.combineMethod, true, 0");

		for (int j=0;j<params.length;j++) { 
			output.print(", p" + j);
		}
		
		output.println(");");

		output.println("\t\t\t}");			
		output.println("\t\t\tbreak;");		

		/* ======================================================================================================================= */	
		output.println();			
		output.println("\t\t\tcase Group.REMOTE:");	
		output.println("\t\t\t{");			

		output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing REMOTE call\");");	

		output.println("\t\t\t\tWriteMessage w = Group.unicast[method.destinationRank].newMessage();");
		output.println("\t\t\t\tw.writeByte(GroupProtocol.INVOCATION);");
		output.println("\t\t\t\tw.writeInt(method.destinationSkeleton);");
		output.println("\t\t\t\tw.writeByte((byte)(Group.REMOTE));");
		output.println("\t\t\t\tw.writeByte((byte)(method.resultMode));");

		output.println("\t\t\t\tw.writeInt(" + number + ");");
		
		output.println("\t\t\t\tswitch (method.resultMode) {");		
		output.println("\t\t\t\tcase Group.COMBINE:");		
		output.println("\t\t\t\t\tw.writeObject(method.combineClass);");
		output.println("\t\t\t\t\tw.writeObject(method.combineMethodName);");
		output.println("\t\t\t\t\t/* fall through */");		
		output.println("\t\t\t\tcase Group.RETURN:");		
		output.println("\t\t\t\t\tticket = Group.ticketMaster.get();");
		output.println("\t\t\t\t\tw.writeInt(Group._rank);");
		output.println("\t\t\t\t\tw.writeInt(ticket);");
		output.println("\t\t\t\t\t/* fall through */");		
		output.println("\t\t\t\tcase Group.DISCARD:");		
		output.println("\t\t\t\t\tbreak;");
		output.println("\t\t\t\t}");
		output.println();

		for (int j=0;j<params.length;j++) { 
			output.println(writeMessageType("\t\t\t\t", "w", params[j], "p" + j));
		}
		
		output.println("\t\t\t\tw.send();");
		output.println("\t\t\t\tw.finish();");

		output.println("\t\t\t\tif (method.resultMode != Group.DISCARD) {");
		output.println("\t\t\t\t\tReadMessage r = (ReadMessage) Group.ticketMaster.collect(ticket);");

		if (!ret.equals(Void.TYPE)) { 
			output.println(readMessageType("\t\t\t\t\t", "result", "r", ret));
		} 		
		output.println("\t\t\t\t\tr.finish();");
		output.println("\t\t\t\t}");
		
		output.println("\t\t\t}");					
		output.println("\t\t\tbreak;");			

		/* ======================================================================================================================= */
		output.println();			
		output.println("\t\t\tcase Group.GROUP:");	
		output.println("\t\t\t{");			

		output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing GROUP call\");");	

		output.println("\t\t\t\tWriteMessage w = Group.multicast.newMessage();");
		output.println("\t\t\t\tw.writeByte(GroupProtocol.INVOCATION);");
		output.println("\t\t\t\tw.writeInt(groupID);");
		output.println("\t\t\t\tw.writeByte((byte)(Group.GROUP));");
		output.println("\t\t\t\tw.writeByte((byte)(method.resultMode));");

		output.println("\t\t\t\tw.writeInt(" + number + ");");

		output.println("\t\t\t\tswitch (method.resultMode) {");		
		output.println("\t\t\t\tcase Group.COMBINE:");		
		output.println("\t\t\t\t\tw.writeObject(method.combineClass);");
		output.println("\t\t\t\t\tw.writeObject(method.combineMethodName);");
		output.println("\t\t\t\t\t/* fall through */");		
		output.println("\t\t\t\tcase Group.RETURN:");		
		output.println("\t\t\t\t\tticket = Group.ticketMaster.get();");
		output.println("\t\t\t\t\tw.writeInt(Group._rank);");
		output.println("\t\t\t\t\tw.writeInt(ticket);");
		output.println("\t\t\t\t\t/* fall through */");		
		output.println("\t\t\t\tcase Group.DISCARD:");		
		output.println("\t\t\t\t\tbreak;");
		output.println("\t\t\t\t}");
		output.println();

		for (int j=0;j<params.length;j++) { 
			output.println(writeMessageType("\t\t\t\t", "w", params[j], "p" + j));
		}
		
		output.println("\t\t\t\tw.send();");
		output.println("\t\t\t\tw.finish();");

		output.println("\t\t\t\tif (method.resultMode != Group.DISCARD) {");
		output.println("\t\t\t\t\tReadMessage r = (ReadMessage) Group.ticketMaster.collect(ticket);");

		if (!ret.equals(Void.TYPE)) { 
			output.println(readMessageType("\t\t\t\t\t", "result", "r", ret));
		} 		
		output.println("\t\t\t\t\tr.finish();");
		output.println("\t\t\t\t}");
		
		output.println("\t\t\t}");					
		output.println("\t\t\tbreak;");			

		/* ======================================================================================================================= */
		output.println();			
		output.println("\t\t\tcase Group.PERSONALIZE:");
		output.println("\t\t\t{");			
		output.println("\t\t\t}");						
		output.println("\t\t\tbreak;");			

		/* ======================================================================================================================= */
		output.println();			
		output.println("\t\t\tdefault:");
		output.println("\t\t\t{");			
		output.println("\t\t\t\tSystem.out.println(\"OOPS : group_stub got illegal opcode\");");
		output.println("\t\t\t\tSystem.exit(1);");
		output.println("\t\t\t}");						
		output.println("\t\t\tbreak;");

		output.println("\t\t\t}");	       
		output.println("\t\t} catch (Exception e) {");
		output.println("\t\t\tSystem.out.println(\"OOPS : \" + e);");
		output.println("\t\t\tSystem.exit(1);");
		output.println("\t\t}");
	} 

	void methodTrailer(Method m) { 
		
		Class ret = m.getReturnType();

		if (!ret.equals(Void.TYPE)) {       
			output.println("\t\treturn result;"); 
		} 
		
		output.println("\t}\n");			
	} 

	void header() { 

		Class [] interfaces = data.subject.getInterfaces();

		if (data.packagename != null) { 
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.group.*;");		
		output.println("import ibis.ipl.*;");		
		output.println();

		output.print("public final class group_stub_" + data.classname + " extends ibis.group.GroupStub implements ");		

		for (int i=0;i<interfaces.length;i++) { 
			output.print(interfaces[i].getName());

			if (i<interfaces.length-1) { 
				output.print(", ");
			}  
		}
			
		output.println(" {\n");
	} 

	void constructor(Vector methods) { 

		output.println("\tpublic group_stub_" + data.classname + "() {");

		output.println("\t\tsuper(" + methods.size() + ");");
		output.println();

		for (int i=0;i<methods.size();i++) { 

			Method m = (Method) methods.get(i);

			Class ret = m.getReturnType();
			Class [] params = m.getParameterTypes();

			output.println("\t\tmethods[" + i + "] = new GroupMethod();");

			output.println("\t\tmethods[" + i + "].name = \"" + m.getName() + "\";");
			output.println("\t\tmethods[" + i + "].returnType = " + getType(ret) + ".class;");
			output.println("\t\tmethods[" + i + "].parameters = new Class[" + params.length + "];");

			for (int j=0;j<params.length;j++) { 
				output.println("\t\tmethods[" + i + "].parameters[" + j + "] = " + getType(params[j]) + ".class;");
			}

			output.print("\t\tmethods[" + i + "].description = \"" + getType(ret) + " " + m.getName() + "(");
			for (int j=0;j<params.length;j++) { 
				output.print(getType(params[j]));

				if (j<params.length-1) { 
					output.print(", ");
				} 
			}

			output.println(")\";");

			output.println("\t\tmethods[" + i + "].invocationMode = ibis.group.Group.LOCAL;");
			output.println("\t\tmethods[" + i + "].resultMode = ibis.group.Group.RETURN;");

			output.println("\t\tmethods[" + i + "].destinationMember   = -1;");
			output.println("\t\tmethods[" + i + "].destinationRank     = -1;");
			output.println("\t\tmethods[" + i + "].destinationSkeleton = -1;");

			output.println("\t\tmethods[" + i + "].personalizeParameters = new Class[" + (params.length+1) + "];");
			output.println("\t\tmethods[" + i + "].personalizeParameters[0] = ibis.group.ParameterVector.class;");

			for (int j=0;j<params.length;j++) { 
				output.println("\t\tmethods[" + i + "].personalizeParameters[" + (j+1) + "] = " + getType(params[j]) + ".class;");
			}

			output.println("\t\tmethods[" + i + "].personalizeClass      = null;");
			output.println("\t\tmethods[" + i + "].personalizeMethodName = null;");
			output.println("\t\tmethods[" + i + "].personalizeMethod     = null;");

			if (!ret.equals(Void.TYPE)) {       
				output.println("\t\tmethods[" + i + "].combineParameters = new Class[5];");
				output.println("\t\tmethods[" + i + "].combineParameters[0] = " + getType(ret) + ".class;"); 
				output.println("\t\tmethods[" + i + "].combineParameters[1] = int.class;");
				output.println("\t\tmethods[" + i + "].combineParameters[2] = " + getType(ret) + ".class;");
				output.println("\t\tmethods[" + i + "].combineParameters[3] = int.class;");
				output.println("\t\tmethods[" + i + "].combineParameters[4] = int.class;");
			} else { 
				output.println("\t\tmethods[" + i + "].combineParameters = new Class[3];");
				output.println("\t\tmethods[" + i + "].combineParameters[0] = int.class;");
				output.println("\t\tmethods[" + i + "].combineParameters[1] = int.class;");
				output.println("\t\tmethods[" + i + "].combineParameters[2] = int.class;");
			}
			output.println("\t\tmethods[" + i + "].combineClass      = null;");
			output.println("\t\tmethods[" + i + "].combineMethodName = null;");
			output.println("\t\tmethods[" + i + "].combineMethod     = null;");


			output.println();			
		}

		output.println("\t}\n");
	} 

	void body(Vector methods) { 

		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			
			methodHeader(m);
			methodBody(m, i);
			methodTrailer(m);		
		} 
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 

	void generate() { 		
		header();		
		constructor(data.specialMethods);		
		body(data.specialMethods);
		trailer();
	} 

} 

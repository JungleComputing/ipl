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
	    output.println("\t\t" + getInitedLocal(ret, "result" ) + ";");
	} 
    }

    void invokeSpecial(String spacing, Method m, Class ret, Class [] params, String pre) { 

	if (!ret.equals(Void.TYPE)) { 
	    output.print(spacing + "return ");
	} else { 
	    output.print(spacing);
	} 
	
	output.print("GMI_" + pre + "_" + m.getName() + "(method");
	
	for (int j=0;j<params.length;j++) { 
	    output.print(", " + " p" + j);
	}
	
	output.println(");");			

	if (ret.equals(Void.TYPE)) { 
	    output.print(spacing + "return;");
	} 		
    } 

    void methodBody(String spacing, Method m, int number) { 
	
	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();

	output.println(spacing + "try {");
	output.println(spacing + "\tGroupMethod method = methods[" + number + "];");			

	output.println(spacing + "\tswitch (method.invocation_mode) {");			

	output.println(spacing + "\tcase InvocationScheme.I_SINGLE:");
	invokeSpecial(spacing + "\t\t", m, ret, params, "SINGLE");
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_GROUP:");
	invokeSpecial(spacing + "\t\t", m, ret, params, "GROUP");
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_PERSONAL:");
	invokeSpecial(spacing + "\t\t", m, ret, params, "PERSONAL");
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_FLAT_SINGLE:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_FLAT_GROUP:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_FLAT_PERSONAL:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_BINARY_SINGLE:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_BINARY_GROUP:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			
	output.println();			

	output.println(spacing + "\tcase InvocationScheme.I_COMBINED_BINARY_PERSONAL:");
	// TODO!!!
	output.println(spacing + "\t\tbreak;");			
	output.println();			

	output.println(spacing + "\tdefault:");
	output.println(spacing + "\t\tSystem.out.println(\"OOPS : group_stub got illegal opcode\");");
	output.println(spacing + "\t\tSystem.exit(1);");
	output.println(spacing + "\t\tbreak;");
	output.println(spacing + "\t}");			

	output.println(spacing + "} catch (Exception e) {");
	output.println(spacing + "\tSystem.out.println(\"OOPS : group_stub got exception\" + e);");
	output.println(spacing + "\te.printStackTrace();");
	output.println(spacing + "\tSystem.exit(1);");
	output.println(spacing + "\t");
	output.println(spacing + "}");			
    }

    void writeAdditionalData(String spacing, int number, Class [] params) { 

	output.println(spacing + "w.writeByte((byte)(method.result_mode));");	
	output.println(spacing + "w.writeInt(" + number + ");");

	output.println(spacing + "switch (method.result_mode) {");		
	output.println(spacing + "case ReplyScheme.R_COMBINE_BINARY:");		
	output.println(spacing + "\tw.writeObject(((CombineReply)(method.rep)).binaryCombiner);");
	output.println(spacing + "\t// fall through");
	output.println(spacing + "case ReplyScheme.R_COMBINE_FLAT:");		
	output.println(spacing + "case ReplyScheme.R_FORWARD:");		
	output.println(spacing + "case ReplyScheme.R_RETURN:");		
	output.println(spacing + "\tw.writeInt(Group._rank);");
	output.println(spacing + "\tticket = replyStack.getPosition();");
	output.println(spacing + "\tw.writeInt(shiftedStubID | ticket);");
	output.println(spacing + "\tbreak;");
	
	output.println(spacing + "}");
	output.println();

	for (int j=0;j<params.length;j++) { 
	    output.println(writeMessageType(spacing, "w", params[j], "p" + j));
	}
	
	output.println(spacing + "w.send();");
	output.println(spacing + "w.finish();");
    } 

    void writeSpecialHeader(String spacing, Method m, String extra) { 
	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();
	
	output.print(spacing + "public final " + getType(ret) + " GMI_" + extra + "_" + m.getName() + "(GroupMethod method");
	
	for (int j=0;j<params.length;j++) { 
	    output.print(", " + getType(params[j]) + " p" + j);
	}
	
	output.print(") throws Exception {\n");
    } 

    void singleMethod(String spacing, Method m, int number) { 

	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();

	writeSpecialHeader(spacing, m, "SINGLE");

	output.println(spacing + "\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing SINGLE call\");");	
	output.println(spacing + "\tGroupMessage r;");
	output.println(spacing + "\tint ticket = 0;");
	output.println(spacing + "\tException ex = null;");			

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\t" + getInitedLocal(ret, "result") + ";");
	}

	output.println(spacing + "\tWriteMessage w = method.sendport.newMessage();");
	output.println(spacing + "\tw.writeByte(GroupProtocol.INVOCATION);");
	output.println(spacing + "\tw.writeInt(method.destinationSkeleton);");
	output.println(spacing + "\tw.writeByte((byte)(InvocationScheme.I_SINGLE));");
	
	writeAdditionalData(spacing + "\t", number, params);		

	output.println(spacing + "\tif (method.result_mode == ReplyScheme.R_FORWARD) {");
	output.println(spacing + "\t\t((ForwardReply) (method.rep)).f.startReceiving(this, size, replyStack, ticket);");
	output.println(spacing + "\t} else {");
	output.println(spacing + "\t\tif (method.result_mode != ReplyScheme.R_DISCARD) {");
	output.println(spacing + "\t\t\tr = (GroupMessage) replyStack.getDataAndFreePosition(ticket);");
	output.println(spacing + "\t\t\tif (r.exceptionResult != null) {");
	output.println(spacing + "\t\t\t\tex = r.exceptionResult;");
	
	if (!ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t\t} else {");
	    if (ret.isPrimitive()) { 
		output.println(spacing + "\t\t\t\tresult = r." + getType(ret) + "Result;");
	    } else { 
		output.println(spacing + "\t\t\t\tresult = (" + getType(ret) + ") r.objectResult;");
	    } 
	} 		
	output.println(spacing + "\t\t\t}");
	output.println(spacing + "\t\tfreeGroupMessage(r);");
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t}");
	
	output.println(spacing + "\tif (ex != null) {");
	output.println(spacing + "\t\tthrow ex;");
	output.println(spacing + "\t}");

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\treturn result;");
	}

	output.println(spacing + "}");
    }

    void handleGroupResult(String spacing, Method m, Class ret) { 

	output.println(spacing + "switch (method.result_mode) {");		

	output.println(spacing + "case ReplyScheme.R_COMBINE_BINARY:");		
	output.println(spacing + "case ReplyScheme.R_RETURN:");		
	output.println(spacing + "\tr = (GroupMessage) replyStack.getDataAndFreePosition(ticket);");

	output.println(spacing + "\tif (r.exceptionResult != null) {");
	output.println(spacing + "\t\tex = r.exceptionResult;");
	
	if (!ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t} else {");
	    if (ret.isPrimitive()) { 
		output.println(spacing + "\t\tresult = r." + getType(ret) + "Result;");
	    } else { 
		output.println(spacing + "\t\tresult = (" + getType(ret) + ") r.objectResult;");
	    } 
	} 		
	output.println(spacing + "\t}");
	output.println(spacing + "\tfreeGroupMessage(r);");
	output.println(spacing + "\tbreak;");

	output.println(spacing + "case ReplyScheme.R_COMBINE_FLAT:");
	if (!ret.equals(Void.TYPE)) { 			
	    if (ret.isPrimitive()) {				
		output.print(spacing + "\t" + getType(ret) + " [] results = new ");
		
		if (ret.equals(Byte.TYPE)) { 
			output.println("byte");
		} else if (ret.equals(Character.TYPE)) { 
			output.println("char");
		} else if (ret.equals(Short.TYPE)) {
			output.println("short");
		} else if (ret.equals(Integer.TYPE)) {
			output.println("int");
		} else if (ret.equals(Long.TYPE)) {
			output.println("long");
		} else if (ret.equals(Float.TYPE)) {
			output.println("float");
		} else if (ret.equals(Double.TYPE)) {
			output.println("double");
		} else if (ret.equals(Boolean.TYPE)) {
			output.println("boolean");
		} 				

		output.println("[size];");
	    } else { 
		output.println(spacing + "\tObject [] results = new Object[size];");
	    } 
	} 

	output.println(spacing + "\tException [] exceptions = new Exception[size];");

	output.println(spacing + "\tfor (int i=0;i<size;i++) {"); 

	output.println(spacing + "\t\tr = (GroupMessage) replyStack.getData(ticket);");

	output.println(spacing + "\t\tif (r.exceptionResult != null) {");		
	output.println(spacing + "\t\t\texceptions[r.rank] = r.exceptionResult;");		

	if (!ret.equals(Void.TYPE)) {
	    output.println(spacing + "\t\t} else {");
	    if (ret.isPrimitive()) { 
		output.println(spacing + "\t\t\tresults[r.rank] = r." + getType(ret) + "Result;");
	    } else { 
		output.println(spacing + "\t\t\tresults[r.rank] = (" + getType(ret) + ") r.objectResult;");
	    } 
	} 		
	output.println(spacing + "\t\t}");
	output.println(spacing + "\t\tfreeGroupMessage(r);");
	output.println(spacing + "\t}");
	output.println(spacing + "\treplyStack.freePosition(ticket);");		

	if (ret.isPrimitive()) { 
	    if (ret.equals(Void.TYPE)) { 			
		output.println(spacing + "\t((CombineReply) (method.rep)).flatCombiner.combine(exceptions);");				
	    } else {
		output.println(spacing + "\tresult = ((CombineReply) (method.rep)).flatCombiner.combine(results, exceptions);");  			
	    }
	} else { 
	    output.println(spacing + "\tresult = (" + getType(ret) + ") ((CombineReply) (method.rep)).flatCombiner.combine(results, exceptions);");
	}
	output.println(spacing + "\tbreak;");

	output.println(spacing + "case ReplyScheme.R_FORWARD:");
	output.println(spacing + "\t((ForwardReply)(method.rep)).f.startReceiving(this, size, replyStack, ticket);");
	output.println(spacing + "\tbreak;");

	output.println(spacing + "case ReplyScheme.R_DISCARD:");
	output.println(spacing + "\tbreak;");
	output.println(spacing + "}");		
    }

    void groupMethod(String spacing, Method m, int number) { 

	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();

	writeSpecialHeader(spacing, m, "GROUP");

	output.println(spacing + "\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing GROUP call\");");	

	output.println(spacing + "\tGroupMessage r;");
	output.println(spacing + "\tint ticket = 0;");
	output.println(spacing + "\tException ex = null;");			

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\t" + getInitedLocal(ret, "result") + ";");
	}

	output.println(spacing + "\tWriteMessage w = method.sendport.newMessage();");
	output.println(spacing + "\tw.writeByte(GroupProtocol.INVOCATION);");
	output.println(spacing + "\tw.writeInt(groupID);");
	output.println(spacing + "\tw.writeByte((byte)(InvocationScheme.I_GROUP));");

	writeAdditionalData(spacing + "\t", number, params);		
	handleGroupResult(spacing + "\t", m, ret);

	output.println(spacing + "\tif (ex != null) {");
	output.println(spacing + "\t\tthrow ex;");
	output.println(spacing + "\t}");

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\treturn result;");
	}

	output.println(spacing + "}");
    } 	
    
    void personalizedMethod(String spacing, Method m, int number) { 

	Class ret = m.getReturnType();
	Class [] params = m.getParameterTypes();

	writeSpecialHeader(spacing, m, "PERSONAL");

	output.println(spacing + "\tif (Group.DEBUG) System.out.println(\"group_stub_" + data.classname + "." + m.getName() + " doing PERSONAL call\");");	

	output.println(spacing + "\tGroupMessage r;");
	output.println(spacing + "\tint ticket = 0;");
	output.println(spacing + "\tboolean haveTicket = false;");
	output.println(spacing + "\tException ex = null;");			
	output.println(spacing + "\tlong memberID;");			
	output.println(spacing + "\tWriteMessage w;");

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\t" + getInitedLocal(ret, "result") + ";");
	}

	output.println(spacing + "\tgroup_parameter_vector_" + data.classname + "_" + m.getName() + "[] pv = new group_parameter_vector_" + data.classname + "_" + m.getName() + "[size];");
	output.println(spacing + "\tgroup_parameter_vector_" + data.classname + "_" + m.getName() + " iv = new group_parameter_vector_" + data.classname + "_" + m.getName() + "();");

	for (int i=0;i<params.length;i++) { 
	    output.println(spacing + "\tiv.write(" + i + ", p" + i + ");");
	} 	

	output.println(spacing + "\tfor (int i=0;i<size;i++) {");
	output.println(spacing + "\t\tpv[i] = new group_parameter_vector_"  + data.classname + "_" + m.getName() + "();");
	output.println(spacing + "\t}");
	
	output.println(spacing + "\ttry {");
	output.println(spacing + "\t\t((PersonalizedInvocation)(method.inv)).p.personalize(iv, pv);");
	output.println(spacing + "\t} catch (Exception e) {");
	output.println(spacing + "\t\tthrow new RuntimeException(\"OOPS: \" + e);");
	output.println(spacing + "\t}");

	output.println(spacing + "\tfor (int i=0;i<size;i++) {");
	output.println(spacing + "\t\tif (!pv[i].done) {"); 
	output.println("\t\t\tthrow new RuntimeException(\"Parameters for groupmember \" + i + \" not completed!!\");");
	output.println(spacing + "\t\t}"); 
	output.println(spacing + "\t}"); 

	output.println(spacing + "\tfor (int i=0;i<size;i++) {");
	output.println(spacing + "\t\tmemberID = memberIDs[i];"); 
	output.println(spacing + "\t\tw = Group.unicast[(int)((memberID >> 32) & 0xFFFFFFFFL)].newMessage();");
	output.println(spacing + "\t\tw.writeByte(GroupProtocol.INVOCATION);");
	output.println(spacing + "\t\tw.writeInt((int) (memberID & 0xFFFFFFFFL));");
	output.println(spacing + "\t\tw.writeByte((byte)(InvocationScheme.I_PERSONAL));");
	output.println(spacing + "\t\tw.writeByte((byte)(method.result_mode));");	
	output.println(spacing + "\t\tw.writeInt(" + number + ");");

	output.println();
	output.println(spacing + "\t\tswitch (method.result_mode) {");		
	output.println(spacing + "\t\tcase ReplyScheme.R_COMBINE_BINARY:");		
	output.println(spacing + "\tw.writeObject(((CombineReply)(method.rep)).binaryCombiner);");
	output.println(spacing + "\t// fall through");
	output.println(spacing + "\t\tcase ReplyScheme.R_COMBINE_FLAT:");		
	output.println(spacing + "\t\tcase ReplyScheme.R_FORWARD:");		
	output.println(spacing + "\t\tcase ReplyScheme.R_RETURN:");		
	output.println(spacing + "\t\t\tw.writeInt(Group._rank);");
	output.println(spacing + "\t\t\tif (!haveTicket) {");
	output.println(spacing + "\t\t\t\tticket = replyStack.getPosition();");
	output.println(spacing + "\t\t\t\thaveTicket = true;");
	output.println(spacing + "\t\t\t}");
	output.println(spacing + "\t\t\tw.writeInt(shiftedStubID | ticket);");
	output.println(spacing + "\t\t\tbreak;");	
	output.println(spacing + "\t\t}");
	output.println();

	for (int j=0;j<params.length;j++) { 
	    if (params[j].isArray()) {
		output.println(spacing + "\t\tif (pv[i].p"+j+"_subarray) {");	
		output.println(spacing + "\t\t\tw.writeSubArray" + printType(params[j].getComponentType()) + "(pv[i].p"+ j + ", pv[i].p" + j + "_offset, pv[i].p" + j + "_size);");
		output.println(spacing + "\t\t}else {");
		output.println(writeMessageType(spacing + "\t\t\t", "w", params[j], "pv[i].p" + j));
		output.println(spacing + "\t\t}");
	    } else { 
		output.println(writeMessageType(spacing + "\t\t", "w", params[j], "pv[i].p" + j));
	    }
	}
	output.println(spacing + "\t\tw.send();");
	output.println(spacing + "\t\tw.finish();");
	output.println(spacing + "\t}");

	handleGroupResult(spacing + "\t", m, ret);

	output.println(spacing + "\tif (ex != null) {");
	output.println(spacing + "\t\tthrow ex;");
	output.println(spacing + "\t}");

	if (!ret.equals(Void.TYPE)) { 	
	    output.println(spacing + "\treturn result;");
	}

	output.println(spacing + "}");
    } 	

    void methodTrailer(Method m) { 
	
	Class ret = m.getReturnType();

	if (!ret.equals(Void.TYPE)) {       
	    output.println("\t\treturn result;"); 
	} 
	
	output.println("\t}\n");			
    } 

    void header() { 

	//Class [] interfaces = data.subject.getInterfaces();

	if (data.packagename != null) { 
	    output.println("package " + data.packagename + ";");		
	    output.println();
	}

	output.println("import ibis.group.*;");		
	output.println("import ibis.ipl.*;");		
	output.println();

	output.print("public final class group_stub_" + data.classname + " extends ibis.group.GroupStub implements ");		
	output.print(data.subject.getName());

	//for (int i=0;i<interfaces.length;i++) { 
	//	output.print(interfaces[i].getName());

	//	if (i<interfaces.length-1) { 
	//		output.print(", ");
	//	}  
	//}
	    
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

	    output.print("\t\tmethods[" + i + "] = new GroupMethod(this, \"");
	    output.print(getType(ret) + " " + m.getName() + "(");
	    for (int j=0;j<params.length;j++) { 
		output.print(getType(params[j]));

		if (j<params.length-1) { 
		    output.print(", ");
		} 
	    }

	    output.println(")\");");

	    output.println("\t\tmethods[" + i + "].invocation_mode = ibis.group.InvocationScheme.I_SINGLE;");
	    output.println("\t\tmethods[" + i + "].result_mode = ibis.group.ReplyScheme.R_RETURN;");

	    output.println("\t\tmethods[" + i + "].destinationSkeleton = -1;");
/*
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
*/
	    output.println();			
	}

	output.println("\t}\n");
    } 

    void body(Vector methods) { 

	for (int i=0;i<methods.size();i++) { 
	    Method m = (Method) methods.get(i);
	
	    methodHeader(m);
	    methodBody("\t\t", m, i);
	    methodTrailer(m);	

	    singleMethod("\t", m, i);
	    groupMethod("\t", m, i);
	    personalizedMethod("\t", m, i);
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

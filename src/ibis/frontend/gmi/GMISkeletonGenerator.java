package ibis.frontend.group;

import java.util.Vector;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import ibis.util.Analyzer;

class GMISkeletonGenerator extends GMIGenerator { 

	Analyzer data;
	PrintWriter output;
	boolean verbose;

	String dest_name;

	GMISkeletonGenerator(Analyzer data, PrintWriter output, boolean verbose) {
		this.data   = data;		
		this.output = output;
		this.verbose = verbose;
	} 
     
	void header() { 
		Class [] interfaces = data.subject.getInterfaces();

		if (data.packagename != null) {
			output.println("package " + data.packagename + ";");		
			output.println();
		}

		output.println("import ibis.group.*;");		
		output.println("import ibis.ipl.*;");		
		output.println("import java.lang.reflect.*;");		
		output.println();
	
		output.println("public final class group_skeleton_" + dest_name + " extends ibis.group.GroupSkeleton {");
		output.println();		
	} 
	void writeResult(String spacing, Class ret) { 
		String resultOpcode = getResultOpcode(ret);

		output.println(spacing + "if (ex != null) {");			
		output.println(spacing + "\tw.writeByte(Group.RESULT_EXCEPTION);");					
		output.println(spacing + "\tw.writeObject(ex);");					
		output.println(spacing + "} else {");			
		output.println(spacing + "\tw.writeByte("+ resultOpcode +");");					

		if (!ret.equals(Void.TYPE)) { 
			output.println(writeMessageType(spacing + "\t", "w", ret, "result"));
		}
		
		output.println(spacing + "}");		
	}

	void handleResult(String spacing, Class ret) { 

//		output.println(spacing + "if ((resultMode != Group.DISCARD) && !(invocationMode != Group.REMOTE && root_object != destination.rank)) {");
	
		output.println(spacing + "switch (resultMode) {");
		
		/* discard the result */
		output.println(spacing + "case Group.DISCARD:");			
		output.println(spacing + "\tbreak;");
		output.println();
		
		/* return the result for unicast, multicast or binarycombine */
		output.println(spacing + "case Group.RETURN:");					
		output.println(spacing + "case Group.BINARYCOMBINE:");	
		output.println(spacing + "\tif (invocationMode == Group.REMOTE || root_object == destination.rank) {");			
		output.println(spacing + "\t\tw = Group.unicast[cpu_rank].newMessage();");			
		output.println(spacing + "\t\tw.writeByte(GroupProtocol.INVOCATION_REPLY);");			
		output.println(spacing + "\t\tw.writeByte((byte) resultMode);");			
		output.println(spacing + "\t\tw.writeInt(ticket);");			
		output.println(spacing + "\t\tw.writeInt(rank);");			

		writeResult(spacing + "\t\t", ret);	

		output.println(spacing + "\t\tw.send();");			
		output.println(spacing + "\t\tw.finish();");						
		output.println(spacing + "\t}");			
		output.println(spacing + "\tbreak;");
		output.println();

		/* return the result for flatcombine or forward */
		output.println(spacing + "case Group.FLATCOMBINE:");
		output.println(spacing + "case Group.FORWARD:");
		output.println(spacing + "\tw = Group.unicast[cpu_rank].newMessage();");			
		output.println(spacing + "\tw.writeByte(GroupProtocol.INVOCATION_REPLY);");			
		output.println(spacing + "\tw.writeByte((byte) resultMode);");			
		output.println(spacing + "\tw.writeInt(ticket);");
		output.println(spacing + "\tw.writeInt(rank);");			

		writeResult(spacing + "\t", ret);	

		output.println(spacing + "\tw.send();");			
		output.println(spacing + "\tw.finish();");						
		output.println(spacing + "\tbreak;");
		output.println();

		/* the default case */
		output.println(spacing + "default:");
		output.println(spacing + "\tSystem.err.println(\"OOPS: group_skeleton got illegal resultMode number!\");");
		output.println(spacing + "\tSystem.exit(1);");
		output.println(spacing + "\tbreak;");
		output.println();
		
		output.println(spacing + "}");
		output.println();
	} 

	void handleMethodInvocation(String spacing, Method m, Class ret, Class [] params) { 

		output.println(spacing + "\t/* Second - Extract the parameters */");		       
			
		for (int j=0;j<params.length;j++) { 
			Class temp = params[j];
			output.println(readMessageType(spacing + "\t", " p" + j, "r", temp, true));
		}
			
		output.println(spacing + "\tr.finish();");
		output.println();
		
		output.println(spacing + "\t/* Third - Invoke the method */");		       		

		output.println(spacing + "\ttry {");
		output.print(spacing + "\t\t");

		if (!ret.equals(Void.TYPE)) { 
			output.print("result = ");
		} 

		output.print("((" + dest_name + ") destination)." + m.getName() + "(");
		
		for (int j=0;j<params.length;j++) { 
			output.print("p" + j);
			if (j < params.length-1) { 
				output.print(", ");
			}
		}

		output.println(");");

		output.println(spacing + "\t} catch (Exception e) {");
		output.println(spacing + "\t\tex = e;");
		output.println(spacing + "\t}");
	} 
	
	void methodHandler(String spacing, Method m) { 

		Class ret = m.getReturnType();
		Class [] params = m.getParameterTypes();

		output.print(spacing + "public final void GMI_" + m.getName() + "(int invocationMode, int resultMode, ReadMessage r) throws IbisException {");
		output.println();			       

		output.println(spacing + "\tint cpu_rank = 0;");		
		output.println(spacing + "\tint root_object = 0;");		
		output.println(spacing + "\tint ticket = 0;");	
		output.println(spacing + "\tBinaryCombiner combiner = null;");
		output.println(spacing + "\tWriteMessage w;");
		output.println(spacing + "\tException ex = null;");

		for (int j=0;j<params.length;j++) { 			
			output.println(spacing + "\t" + getInitedLocal(params[j], "p" + j) + ";");
		}

		if (!ret.equals(Void.TYPE)) { 
			output.println(spacing + "\t" + getInitedLocal(ret, "result") + ";");
		}

		output.println();		

		output.println(spacing + "\t/* First - Read additional data */");
		output.println(spacing + "\tswitch (resultMode) {");
		output.println(spacing + "\tcase Group.BINARYCOMBINE:");		
		output.println(spacing + "\t\tcombiner = (BinaryCombiner) r.readObject();");
//		output.println(spacing + "\t\t/* find root_object on machine cpu_rank */");	       	
		output.println(spacing + "\t\t/* fall through */");		
		output.println(spacing + "\tcase Group.FLATCOMBINE:");		
		output.println(spacing + "\tcase Group.FORWARD:");		
		output.println(spacing + "\tcase Group.RETURN:");		
		output.println(spacing + "\t\tcpu_rank = r.readInt();");		
		output.println(spacing + "\t\tticket   = r.readInt();");		
		output.println(spacing + "\t\t/* fall through */");		
		output.println(spacing + "\tcase Group.DISCARD:");		
		output.println(spacing + "\t\tbreak;");				
		output.println(spacing + "\t}");		

		handleMethodInvocation(spacing, m, ret, params);
		output.println();		

		output.println(spacing + "/* Fourth - Handle the result */");

		output.println(spacing + "\tif (combiner != null) {");
		output.println(spacing + "\t\t/* call combiner here */");
		output.println(spacing + "\t\ttry {");

		if (ret.equals(Void.TYPE)) {
			output.println(spacing + "\t\t\tcombine_void(combiner, (invocationMode == Group.REMOTE), cpu_rank, ex);"); 
		} else {
			if (ret.isPrimitive()) { 
				output.println(spacing + "\t\t\tresult = combine_" + 
					       getType(ret) + "(combiner, (invocationMode == Group.REMOTE), cpu_rank, result, ex);");
			} else { 
				output.println(spacing + "\t\t\tresult = (" + getType(ret) + ") combine_Object(combiner, (invocationMode == Group.REMOTE), cpu_rank, result, ex);");
			}
		} 

		output.println(spacing + "\t\t} catch (Exception e) {");
		output.println(spacing + "\t\t\tex = e;");
		output.println(spacing + "\t\t}");
		output.println(spacing + "\t}");

		output.println();
		handleResult(spacing + "\t", ret);
		
		output.println(spacing + "}");	
		output.println();			       
	} 
	
//  	void methodCombiners() { 
//  		methodCombinerVoid();
//  		methodCombinerObject();
//  	}

//  	void methodCombinerObject() { 

//  		output.print("\tpublic final Object GMI_combine(Combiner resultCombiner, boolean to_all, int lroot, Object local_result) throws IbisException {");
//  		output.println();			       

//  		output.println("\t\tint peer;");
//  		output.println("\t\tint mask = 1;");
//  		output.println("\t\tint size = destination.size;");
//  		output.println("\t\tint rank = destination.rank;");
//  		output.println("\t\tint relrank = (rank - lroot + size) % size;");

//  		output.println("\t\tObject remote_result;"); 

//  		output.println("\t\tObject [] combine_params = { null, new Integer(rank), null, null, new Integer(size) };");

//  		output.println();
//  		output.println("\t\tboolean exception = (local_result instanceof Exception);");

//  		output.println();
//  		output.println("\t\twhile (mask < size) {");
//                    output.println("\t\t\tif ((mask & relrank) == 0) {");
//  		    output.println("\t\t\t\tpeer = relrank | mask;");
//  		    output.println("\t\t\t\tif (peer < size) {");
//  		      output.println("\t\t\t\t\tpeer = (peer + lroot) % size;");
//  		      output.println("\t\t\t\t\t/* receive result */");
//  		      output.println("\t\t\t\t\tremote_result = messageQ.dequeue(peer);");
//  		      output.println("\t\t\t\t\texception = exception || (remote_result instanceof Exception);");
//  		      output.println("\t\t\t\t\tif (!exception) {");
//  		        output.println("\t\t\t\t\t\t/* call combiner */");

//  			output.println("\t\t\t\t\t\tcombine_params[0] = local_result;");
//  			output.println("\t\t\t\t\t\tcombine_params[2] = remote_result;");
//  			output.println("\t\t\t\t\t\tcombine_params[3] = new Integer(peer);");

//  			output.println("\t\t\t\t\t\ttry {");			  
//  			output.println("\t\t\t\t\t\t\tlocal_result = combineMethod.invoke(null, combine_params);");			  
//  			output.println("\t\t\t\t\t\t} catch (InvocationTargetException e1) {");			  
//  			output.println("\t\t\t\t\t\t} catch (IllegalAccessException e2) {");			  
//  			output.println("\t\t\t\t\t\t} catch (IllegalArgumentException e3) {");			  
//  			output.println("\t\t\t\t\t\t} catch (Exception e2) {");			  
//  			output.println("\t\t\t\t\t\t}");			  

//  		      output.println("\t\t\t\t\t} else {");
//  		        output.println("\t\t\t\t\t\tif (!(local_result instanceof Exception)) local_result = (Exception) remote_result;");
//  		      output.println("\t\t\t\t\t}");

//  		    output.println("\t\t\t\t}");
//    	          output.println("\t\t\t} else {");

//  		    output.println("\t\t\t\tpeer = ((relrank & (~mask)) + lroot) % size;");
//  		    output.println("\t\t\t\t/* send result */");

//  		    output.println("\t\t\t\tlong memberID = destination.memberIDs[peer];");
//  		    output.println("\t\t\t\tint peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);");
//  		    output.println("\t\t\t\tint peer_skeleton = (int) (memberID & 0xFFFFFFFFL);");

//  		    output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"Sending message to peer \" + peer + \" on cpu \" + peer_rank);");

//  		    output.println("\t\t\t\tWriteMessage w = Group.unicast[peer_rank].newMessage();");
//  		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
//  		    output.println("\t\t\t\tw.writeInt(peer_skeleton);");
//  		    output.println("\t\t\t\tw.writeInt(rank);");
//  		    output.println("\t\t\t\tw.writeObject(local_result);");
//  		    output.println("\t\t\t\tw.send();");
//  		    output.println("\t\t\t\tw.finish();");
//  		    output.println("\t\t\t\tbreak;");
//  		  output.println("\t\t\t}");
//                    output.println("\t\t\tmask <<= 1;");
//  		output.println("\t\t}");

//  		output.println();
//  		output.println("\t\tif (to_all) {");
//   		  output.println("\t\t\tif (rank == lroot) {");
//  		    output.println("\t\t\t\tif (reply_to_all == null) {");
//                      output.println("\t\t\t\t\treply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);");
//  		    output.println("\t\t\t\t}");

//  		    output.println("\t\t\t\t/* forward result to all */");
//  		    output.println("\t\t\t\tWriteMessage w = reply_to_all.newMessage();");
//  		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE_RESULT);");
//  		    output.println("\t\t\t\tw.writeInt(destination.groupID);");
//  		    output.println("\t\t\t\tw.writeInt(lroot);");
//  		    output.println("\t\t\t\tw.writeObject(local_result);");
//  		    output.println("\t\t\t\tw.send();");
//  		    output.println("\t\t\t\tw.finish();");
//  	          output.println("\t\t\t}");
//  		  output.println("\t\t\t/* receive result from root */");
//  		  output.println("\t\t\tlocal_result = messageQ.dequeue(lroot);");
//  		  output.println("\t\t\texception = exception || (local_result instanceof Exception);");
//  		output.println("\t\t}");

//  		output.println();
//  		output.println("\t\tif (exception) {");
//  		  output.println("\t\t\t/* throw exception here */");
//  		output.println("\t\t}");
//  		output.println();

//  		output.println("\t\treturn local_result;");
	       			
//  		output.println("\t}");	
//  		output.println();			       
//  	} 

//  	void methodCombinerVoid() { 

//  		output.print("\tpublic final void GMI_combine_void(Method combineMethod, boolean to_all, int lroot, Object local_result) throws IbisException {");
//  		output.println();			       

//  		output.println("\t\tint peer;");
//  		output.println("\t\tint mask = 1;");
//  		output.println("\t\tint size = destination.size;");
//  		output.println("\t\tint rank = destination.rank;");
//  		output.println("\t\tint relrank = (rank - lroot + size) % size;");

//  		output.println("\t\tObject remote_result;"); 

//  		output.println("\t\tObject [] combine_params = { new Integer(rank), null, new Integer(size) };");

//  		output.println();
//  		output.println("\t\tboolean exception = (local_result instanceof Exception);");

//  		output.println();
//  		output.println("\t\twhile (mask < size) {");
//                    output.println("\t\t\tif ((mask & relrank) == 0) {");
//  		    output.println("\t\t\t\tpeer = relrank | mask;");
//  		    output.println("\t\t\t\tif (peer < size) {");
//  		      output.println("\t\t\t\t\tpeer = (peer + lroot) % size;");
//  		      output.println("\t\t\t\t\t/* receive result */");
//  		      output.println("\t\t\t\t\tremote_result = messageQ.dequeue(peer);");
//  		      output.println("\t\t\t\t\texception = exception || (remote_result instanceof Exception);");
//  		      output.println("\t\t\t\t\tif (!exception) {");
//  		        output.println("\t\t\t\t\t\t/* call combiner */");

//  			output.println("\t\t\t\t\t\tcombine_params[1] = new Integer(peer);");

//  			output.println("\t\t\t\t\t\ttry {");			  
//  			output.println("\t\t\t\t\t\t\tcombineMethod.invoke(null, combine_params);");			  
//  			output.println("\t\t\t\t\t\t} catch (InvocationTargetException e1) {");			  
//  			output.println("\t\t\t\t\t\t} catch (IllegalAccessException e2) {");			  
//  			output.println("\t\t\t\t\t\t} catch (IllegalArgumentException e3) {");			  
//  			output.println("\t\t\t\t\t\t} catch (Exception e2) {");			  
//  			output.println("\t\t\t\t\t\t}");			  

//  		      output.println("\t\t\t\t\t} else {");
//  		        output.println("\t\t\t\t\t\tif (!(local_result instanceof Exception)) local_result = (Exception) remote_result;");
//  		      output.println("\t\t\t\t\t}");

//  		    output.println("\t\t\t\t}");
//    	          output.println("\t\t\t} else {");
//  		    output.println("\t\t\t\tpeer = ((relrank & (~mask)) + lroot) % size;");
//  		    output.println("\t\t\t\t/* send result */");

//  		    output.println("\t\t\t\tlong memberID = destination.memberIDs[peer];");
//  		    output.println("\t\t\t\tint peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);");
//  		    output.println("\t\t\t\tint peer_skeleton = (int) (memberID & 0xFFFFFFFFL);");

//  		    output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"Sending message to peer \" + peer + \" on cpu \" + peer_rank);");		    		   

//  		    output.println("\t\t\t\tWriteMessage w = Group.unicast[peer_rank].newMessage();");
//  		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
//  		    output.println("\t\t\t\tw.writeInt(peer_skeleton);");
//  		    output.println("\t\t\t\tw.writeInt(rank);");
//  		    output.println("\t\t\t\tw.writeObject(local_result);");
//  		    output.println("\t\t\t\tw.send();");
//  		    output.println("\t\t\t\tw.finish();");
//  		    output.println("\t\t\t\tbreak;");
//  		  output.println("\t\t\t}");
//                    output.println("\t\t\tmask <<= 1;");
//  		output.println("\t\t}");

//  		output.println();
//  		output.println("\t\tif (to_all) {");
//   		  output.println("\t\t\tif (rank == lroot) {");
//  		    output.println("\t\t\t\t/* forward result to all */");
//  		    output.println("\t\t\t\tif (reply_to_all == null) {");
//                      output.println("\t\t\t\t\treply_to_all = Group.getMulticastSendport(destination.multicastHostsID, destination.multicastHosts);");
//  		    output.println("\t\t\t\t}");
		    
//  		    output.println("\t\t\t\tWriteMessage w = reply_to_all.newMessage();");
//  		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
//  		    output.println("\t\t\t\tw.writeObject(local_result);");
//  		    output.println("\t\t\t\tw.send();");
//  		    output.println("\t\t\t\tw.finish();");
//  	          output.println("\t\t\t}");
//  		  output.println("\t\t\t/* receive result from root */");
//  		  output.println("\t\t\tlocal_result = messageQ.dequeue(lroot);");
//  		  output.println("\t\t\texception = exception || (local_result instanceof Exception);");
//  		output.println("\t\t}");

//  		output.println();
//  		output.println("\t\tif (exception) {");
//  		  output.println("\t\t\t/* throw exception here */");
//  		output.println("\t\t}");
//  		output.println();

//  		output.println("\t\treturn;");
	       			
//  		output.println("\t}");	
//  		output.println();			       
//  	} 

	String getResultOpcode(Class ret) { 

		String result = null;

		if (ret.isPrimitive()) {				
			if (ret.equals(Byte.TYPE)) { 
				result = "Group.RESULT_BYTE";
			} else if (ret.equals(Void.TYPE)) { 
				result = "Group.RESULT_VOID";
			} else if (ret.equals(Character.TYPE)) { 
				result = "Group.RESULT_CHAR";
			} else if (ret.equals(Short.TYPE)) {
				result = "Group.RESULT_SHORT";
			} else if (ret.equals(Integer.TYPE)) {
				result = "Group.RESULT_INT";
			} else if (ret.equals(Long.TYPE)) {
				result = "Group.RESULT_LONG";
			} else if (ret.equals(Float.TYPE)) {
				result = "Group.RESULT_FLOAT";
			} else if (ret.equals(Double.TYPE)) {
				result = "Group.RESULT_DOUBLE";
			} else if (ret.equals(Boolean.TYPE)) {
				result = "Group.RESULT_BOOLEAN";
			} 					       
		} else { 
			result = "Group.RESULT_OBJECT";
		} 		

		return result;
	}



	void messageHandler(String spacing, Vector methods) { 

		output.println(spacing + "public final void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException {");
		output.println();			
		output.println(spacing + "\tint method = r.readInt();");	

		output.println(spacing + "\tswitch(method) {");		
	
		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			output.println(spacing + "\tcase " + i + ":");
			output.println(spacing + "\t\tGMI_" + m.getName() + "(invocationMode, resultMode, r);");
			output.println(spacing + "\t\tbreak;");
		}
		
		output.println(spacing + "\tdefault:");
		output.println(spacing + "\t\tSystem.err.println(\"OOPS: group_skeleton got illegal method number!\");");
		output.println(spacing + "\t\tSystem.exit(1);");
		output.println(spacing + "\t\tbreak;");
		
		output.println(spacing + "\t}");
		output.println(spacing + "}");
	}
	       
	void trailer() { 
		output.println("}\n");
	} 
       
	void constructor(String spacing, Vector methods) { 

		output.println(spacing + "public group_skeleton_" + data.classname + "() {");

		output.println(spacing + "\tsuper(" + methods.size() + ");");
		output.println();

		for (int i=0;i<methods.size();i++) { 

			Method m = (Method) methods.get(i);

			Class ret = m.getReturnType();
			Class [] params = m.getParameterTypes();

			output.println(spacing + "\tmethods[" + i + "] = new GroupMethod(this);");

			output.println(spacing + "\tmethods[" + i + "].name = \"" + m.getName() + "\";");
			output.println(spacing + "\tmethods[" + i + "].returnType = " + getType(ret) + ".class;");
			output.println(spacing + "\tmethods[" + i + "].parameters = new Class[" + params.length + "];");

			for (int j=0;j<params.length;j++) { 
				output.println(spacing + "\tmethods[" + i + "].parameters[" + j + "] = " + getType(params[j]) + ".class;");
			}

			output.print(spacing + "\tmethods[" + i + "].description = \"" + getType(ret) + " " + m.getName() + "(");
			for (int j=0;j<params.length;j++) { 
				output.print(getType(params[j]));

				if (j<params.length-1) { 
					output.print(", ");
				} 
			}

			output.println(")\";");

			output.println(spacing + "\tmethods[" + i + "].personalizeParameters = new Class[" + (params.length+1) + "];");

			for (int j=0;j<params.length;j++) { 
				output.println(spacing + "\tmethods[" + i + "].personalizeParameters[" + (j) + "] = " + getType(params[j]) + ".class;");
			}
			output.println(spacing + "\tmethods[" + i + "].personalizeParameters[" + params.length +"] = ibis.group.ParameterVector[].class;");


/*
			if (!ret.equals(Void.TYPE)) {       
				output.println(spacing + "\tmethods[" + i + "].combineParameters = new Class[5];");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[0] = " + getType(ret) + ".class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[1] = int.class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[2] = " + getType(ret) + ".class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[3] = int.class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[4] = int.class;");
			} else { 
				output.println(spacing + "\tmethods[" + i + "].combineParameters = new Class[3];");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[0] = int.class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[1] = int.class;");
				output.println(spacing + "\tmethods[" + i + "].combineParameters[2] = int.class;");
			}
			output.println();			
*/
		}

		output.println(spacing + "}\n");
	} 

	void body(String spacing, Vector methods) { 
		for (int i=0;i<methods.size();i++) { 
			methodHandler(spacing, (Method) methods.get(i));
		}
	}

	void generate() { 
		dest_name = data.classname;
		header();		
		constructor("\t", data.specialMethods);
		messageHandler("\t", data.specialMethods);
		body("\t", data.specialMethods);	
		trailer();
	} 
} 

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

	void methodHandler(Method m) { 

		Class ret = m.getReturnType();
		Class [] params = m.getParameterTypes();

		output.print("\tpublic final " + getType(ret) + " GMI_" + m.getName() + "(Method combineMethod, boolean to_all, int lroot");
		
		for (int j=0;j<params.length;j++) { 			
			output.print(", " + getType(params[j]) + " p" + j);
		}

		output.println(") throws IbisException {");
		output.println();			       

		//		output.print("\t\tException exception = null;");
		//		output.print();

		output.print("\t\t");
		
		if (!ret.equals(Void.TYPE)) { 
			if (ret.isPrimitive()) {					
				output.print(getType(ret) + " result = ");
			} else { 
				output.print(getType(ret) + " result = ");
			}		
		} 
		
		output.print("((" + dest_name + ") destination)." + m.getName() + "(");
		
		for (int j=0;j<params.length;j++) { 
			output.print("p" + j);
			
			if (j<params.length-1) { 
				output.print(", ");
			}
		}
		output.println(");");			       
		output.println();
		
		output.println("\t\tif (combineMethod != null) {");
		output.println("\t\t\t/* call combiner here */");

		if (ret.isPrimitive()) { 
			if (!ret.equals(Void.TYPE)) {
				output.println("\t\t\t" + containerType(ret) + " resultObject = new " + containerType(ret) + "(result);");
				output.println("\t\t\tresultObject = (" + containerType(ret) + ") GMI_combine(combineMethod, to_all, lroot, resultObject);");
				output.println("\t\t\tresult = resultObject." + getType(ret) + "Value();");				
			} else {
				output.println("\t\t\tGMI_combine_void(combineMethod, to_all, lroot, null);");
			}
		} else { 
			output.println("\t\t\tresult = (" + getType(ret) + ") GMI_combine(combineMethod, to_all, lroot, result);");
		}

		output.println("\t\t}");
		output.println();
		
		if (ret.equals(Void.TYPE)) { 
			output.println("\t\treturn;");
		} else { 
			output.println("\t\treturn result;");
		}
	       			
		output.println("\t}");	
		output.println();			       
	} 
	
	void methodCombiners() { 
		methodCombinerVoid();
		methodCombinerObject();
	}

	void methodCombinerObject() { 

		output.print("\tpublic final Object GMI_combine(Method combineMethod, boolean to_all, int lroot, Object local_result) throws IbisException {");
		output.println();			       

		output.println("\t\tint peer;");
		output.println("\t\tint mask = 1;");
		output.println("\t\tint size = destination.size;");
		output.println("\t\tint rank = destination.rank;");
		output.println("\t\tint relrank = (rank - lroot + size) % size;");

		output.println("\t\tObject remote_result;"); 

		output.println("\t\tObject [] combine_params = { null, new Integer(rank), null, null, new Integer(size) };");

		output.println();
		output.println("\t\tboolean exception = (local_result instanceof Exception);");

		output.println();
		output.println("\t\twhile (mask < size) {");
                  output.println("\t\t\tif ((mask & relrank) == 0) {");
		    output.println("\t\t\t\tpeer = relrank | mask;");
		    output.println("\t\t\t\tif (peer < size) {");
		      output.println("\t\t\t\t\tpeer = (peer + lroot) % size;");
		      output.println("\t\t\t\t\t/* receive result */");
		      output.println("\t\t\t\t\tremote_result = messageQ.dequeue(peer);");
		      output.println("\t\t\t\t\texception = exception || (remote_result instanceof Exception);");
		      output.println("\t\t\t\t\tif (!exception) {");
		        output.println("\t\t\t\t\t\t/* call combiner */");

			output.println("\t\t\t\t\t\tcombine_params[0] = local_result;");
			output.println("\t\t\t\t\t\tcombine_params[2] = remote_result;");
			output.println("\t\t\t\t\t\tcombine_params[3] = new Integer(peer);");

			output.println("\t\t\t\t\t\ttry {");			  
			output.println("\t\t\t\t\t\t\tlocal_result = combineMethod.invoke(null, combine_params);");			  
			output.println("\t\t\t\t\t\t} catch (InvocationTargetException e1) {");			  
			output.println("\t\t\t\t\t\t} catch (IllegalAccessException e2) {");			  
			output.println("\t\t\t\t\t\t} catch (IllegalArgumentException e3) {");			  
			output.println("\t\t\t\t\t\t} catch (Exception e2) {");			  
			output.println("\t\t\t\t\t\t}");			  

		      output.println("\t\t\t\t\t} else {");
		        output.println("\t\t\t\t\t\tif (!(local_result instanceof Exception)) local_result = (Exception) remote_result;");
		      output.println("\t\t\t\t\t}");

		    output.println("\t\t\t\t}");
  	          output.println("\t\t\t} else {");

		    output.println("\t\t\t\tpeer = ((relrank & (~mask)) + lroot) % size;");
		    output.println("\t\t\t\t/* send result */");

		    output.println("\t\t\t\tlong memberID = destination.memberIDs[peer];");
		    output.println("\t\t\t\tint peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);");
		    output.println("\t\t\t\tint peer_skeleton = (int) (memberID & 0xFFFFFFFFL);");

		    output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"Sending message to peer \" + peer + \" on cpu \" + peer_rank);");

		    output.println("\t\t\t\tWriteMessage w = Group.unicast[peer_rank].newMessage();");
		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
		    output.println("\t\t\t\tw.writeInt(peer_skeleton);");
		    output.println("\t\t\t\tw.writeInt(rank);");
		    output.println("\t\t\t\tw.writeObject(local_result);");
		    output.println("\t\t\t\tw.send();");
		    output.println("\t\t\t\tw.finish();");
		    output.println("\t\t\t\tbreak;");
		  output.println("\t\t\t}");
                  output.println("\t\t\tmask <<= 1;");
		output.println("\t\t}");

		output.println();
		output.println("\t\tif (to_all) {");
 		  output.println("\t\t\tif (rank == lroot) {");
		    output.println("\t\t\t\t/* forward result to all */");
		    output.println("\t\t\t\tWriteMessage w = Group.multicast.newMessage();");
		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE_RESULT);");
		    output.println("\t\t\t\tw.writeInt(destination.groupID);");
		    output.println("\t\t\t\tw.writeInt(lroot);");
		    output.println("\t\t\t\tw.writeObject(local_result);");
		    output.println("\t\t\t\tw.send();");
		    output.println("\t\t\t\tw.finish();");
	          output.println("\t\t\t}");
		  output.println("\t\t\t/* receive result from root */");
		  output.println("\t\t\tlocal_result = messageQ.dequeue(lroot);");
		  output.println("\t\t\texception = exception || (local_result instanceof Exception);");
		output.println("\t\t}");

		output.println();
		output.println("\t\tif (exception) {");
		  output.println("\t\t\t/* throw exception here */");
		output.println("\t\t}");
		output.println();

		output.println("\t\treturn local_result;");
	       			
		output.println("\t}");	
		output.println();			       
	} 

	void methodCombinerVoid() { 

		output.print("\tpublic final void GMI_combine_void(Method combineMethod, boolean to_all, int lroot, Object local_result) throws IbisException {");
		output.println();			       

		output.println("\t\tint peer;");
		output.println("\t\tint mask = 1;");
		output.println("\t\tint size = destination.size;");
		output.println("\t\tint rank = destination.rank;");
		output.println("\t\tint relrank = (rank - lroot + size) % size;");

		output.println("\t\tObject remote_result;"); 

		output.println("\t\tObject [] combine_params = { new Integer(rank), null, new Integer(size) };");

		output.println();
		output.println("\t\tboolean exception = (local_result instanceof Exception);");

		output.println();
		output.println("\t\twhile (mask < size) {");
                  output.println("\t\t\tif ((mask & relrank) == 0) {");
		    output.println("\t\t\t\tpeer = relrank | mask;");
		    output.println("\t\t\t\tif (peer < size) {");
		      output.println("\t\t\t\t\tpeer = (peer + lroot) % size;");
		      output.println("\t\t\t\t\t/* receive result */");
		      output.println("\t\t\t\t\tremote_result = messageQ.dequeue(peer);");
		      output.println("\t\t\t\t\texception = exception || (remote_result instanceof Exception);");
		      output.println("\t\t\t\t\tif (!exception) {");
		        output.println("\t\t\t\t\t\t/* call combiner */");

			output.println("\t\t\t\t\t\tcombine_params[1] = new Integer(peer);");

			output.println("\t\t\t\t\t\ttry {");			  
			output.println("\t\t\t\t\t\t\tcombineMethod.invoke(null, combine_params);");			  
			output.println("\t\t\t\t\t\t} catch (InvocationTargetException e1) {");			  
			output.println("\t\t\t\t\t\t} catch (IllegalAccessException e2) {");			  
			output.println("\t\t\t\t\t\t} catch (IllegalArgumentException e3) {");			  
			output.println("\t\t\t\t\t\t} catch (Exception e2) {");			  
			output.println("\t\t\t\t\t\t}");			  

		      output.println("\t\t\t\t\t} else {");
		        output.println("\t\t\t\t\t\tif (!(local_result instanceof Exception)) local_result = (Exception) remote_result;");
		      output.println("\t\t\t\t\t}");

		    output.println("\t\t\t\t}");
  	          output.println("\t\t\t} else {");
		    output.println("\t\t\t\tpeer = ((relrank & (~mask)) + lroot) % size;");
		    output.println("\t\t\t\t/* send result */");

		    output.println("\t\t\t\tlong memberID = destination.memberIDs[peer];");
		    output.println("\t\t\t\tint peer_rank =  (int) ((memberID >> 32) & 0xFFFFFFFFL);");
		    output.println("\t\t\t\tint peer_skeleton = (int) (memberID & 0xFFFFFFFFL);");

		    output.println("\t\t\t\tif (Group.DEBUG) System.out.println(\"Sending message to peer \" + peer + \" on cpu \" + peer_rank);");		    		   

		    output.println("\t\t\t\tWriteMessage w = Group.unicast[peer_rank].newMessage();");
		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
		    output.println("\t\t\t\tw.writeInt(peer_skeleton);");
		    output.println("\t\t\t\tw.writeInt(rank);");
		    output.println("\t\t\t\tw.writeObject(local_result);");
		    output.println("\t\t\t\tw.send();");
		    output.println("\t\t\t\tw.finish();");
		    output.println("\t\t\t\tbreak;");
		  output.println("\t\t\t}");
                  output.println("\t\t\tmask <<= 1;");
		output.println("\t\t}");

		output.println();
		output.println("\t\tif (to_all) {");
 		  output.println("\t\t\tif (rank == lroot) {");
		    output.println("\t\t\t\t/* forward result to all */");
		    output.println("\t\t\t\tWriteMessage w = Group.multicast.newMessage();");
		    output.println("\t\t\t\tw.writeByte(GroupProtocol.COMBINE);");
		    output.println("\t\t\t\tw.writeObject(local_result);");
		    output.println("\t\t\t\tw.send();");
		    output.println("\t\t\t\tw.finish();");
	          output.println("\t\t\t}");
		  output.println("\t\t\t/* receive result from root */");
		  output.println("\t\t\tlocal_result = messageQ.dequeue(lroot);");
		  output.println("\t\t\texception = exception || (local_result instanceof Exception);");
		output.println("\t\t}");

		output.println();
		output.println("\t\tif (exception) {");
		  output.println("\t\t\t/* throw exception here */");
		output.println("\t\t}");
		output.println();

		output.println("\t\treturn;");
	       			
		output.println("\t}");	
		output.println();			       
	} 

	void body(Vector methods) { 

		methodCombiners();

		for (int i=0;i<methods.size();i++) { 
			methodHandler((Method) methods.get(i));
		}
	}

	void messageHandler(Vector methods) { 

		output.println("\tpublic final void handleMessage(int invocationMode, int resultMode, ReadMessage r) throws IbisException {");
		output.println();		

		output.println("\t\tint cpu_rank = 0;");		
		output.println("\t\tint root_object = 0;");		
		output.println("\t\tint ticket = 0;");	
		output.println("\t\tClass combineClass = null;");	
		output.println("\t\tString combineName = null;");	
		output.println("\t\tMethod combineMethod = null;");	
		output.println();		

		output.println("\t\tint method = r.readInt();");		
		output.println();		
		
		output.println("\t\tswitch (resultMode) {");
		output.println("\t\tcase Group.COMBINE:");		
		output.println("\t\t\tcombineClass = (Class)  r.readObject();");		
		output.println("\t\t\tcombineName  = (String) r.readObject();");		
		output.println("\t\t\tcombineMethod = Group.findMethod(combineClass, combineName, methods[method].combineParameters);");		
		output.println("\t\t\t/* find root_object on machine cpu_rank */");	       
		
		output.println("\t\t\t/* fall through */");		
		output.println("\t\tcase Group.RETURN:");		
		output.println("\t\t\tcpu_rank = r.readInt();");		
		output.println("\t\t\tticket   = r.readInt();");		
		output.println("\t\t\t/* fall through */");		
		output.println("\t\tcase Group.DISCARD:");		
		output.println("\t\t\tbreak;");				
		output.println("\t\t}");		

		output.println("\t\tswitch(method) {");		

		for (int i=0;i<methods.size();i++) { 
			Method m = (Method) methods.get(i);
			Class ret = m.getReturnType();
			Class [] params = m.getParameterTypes();
			
			output.println("\t\tcase " + i + ":");
			
			//			output.print("\t\t\t/* " + getType(ret) + " " + m.getName() + "(");			
			//			for (int j=0;j<params.length;j++) { 
			//				output.print(params[j].getName() + " p" + j);
			//				
			//				if (j < params.length-1) { 
			//					output.print(", ");
			//				}
			//			}
			//			output.println(") */");									
			output.println("\t\t{");		       

			output.println("\t\t\t/* First - Extract the parameters */");		       
			
			for (int j=0;j<params.length;j++) { 
				Class temp = params[j];
				/*				
				output.print("\t\t\t");

				if (temp.isPrimitive()) {								
					if (temp.equals(Byte.TYPE)) { 
						output.println("byte p" + j + " = r.readByte();");
					} else if (temp.equals(Character.TYPE)) { 
						output.println("char p" + j + " = r.readChar();");
					} else if (temp.equals(Short.TYPE)) {
						output.println("short p" + j + " = r.readShort();");
					} else if (temp.equals(Integer.TYPE)) {
						output.println("int p" + j + " = r.readInt();");
					} else if (temp.equals(Long.TYPE)) {
						output.println("long p" + j + " = r.readLong();");
					} else if (temp.equals(Float.TYPE)) {
						output.println("float p" + j + " = r.readFloat();");
					} else if (temp.equals(Double.TYPE)) {
						output.println("double p" + j + " = r.readDouble();");
					} else if (temp.equals(Boolean.TYPE)) {
						output.println("boolean p" + j + " = r.readBoolean();");
					} 					       
				} else { 
					output.println("Object p" + j + " = r.readObject();");
				} 
				*/
				output.println(readMessageType("\t\t\t", getType(temp) + " p" + j, "r", temp));
			}
			
			output.println("\t\t\tr.finish();");
			output.println();
			
			output.println("\t\t\t/* Second - Invoke the methodHandler */");		       

			output.print("\t\t\t");
			
			if (!ret.equals(Void.TYPE)) { 
				if (ret.isPrimitive()) {					
					output.print(getType(ret) + " result = ");
				} else { 
					output.print(getType(ret) + " result = ");
				}		
			} 
		
  			output.print("GMI_" + m.getName() + "(combineMethod, false, root_object");
		
			for (int j=0;j<params.length;j++) { 
				output.print(", p" + j);
			}

			output.println(");");
			output.println();


			output.println("\t\t\t/* Third - Handle the result */");		
       
			output.println("\t\t\tswitch (resultMode) {");
			output.println("\t\t\tcase Group.DISCARD:");			
			output.println("\t\t\t\tbreak;");
			output.println();

			output.println("\t\t\tcase Group.RETURN:");			
			
			output.println("\t\t\t\tif (invocationMode == Group.REMOTE || root_object == destination.rank) {");			
			
			output.println("\t\t\t\t\tWriteMessage w = Group.unicast[cpu_rank].newMessage();");			
			output.println("\t\t\t\t\tw.writeByte(GroupProtocol.REPLY);");			
			output.println("\t\t\t\t\tw.writeInt(ticket);");			

			if (!ret.equals(Void.TYPE)) { 
				output.println(writeMessageType("\t\t\t\t\t", "w", ret, "result"));
			}

			output.println("\t\t\t\t\tw.send();");			
			output.println("\t\t\t\t\tw.finish();");						
			output.println("\t\t\t\t}");			
			
			output.println("\t\t\t\tbreak;");
			output.println();

			output.println("\t\t\tcase Group.COMBINE:");	

			output.println("\t\t\t\tif (invocationMode == Group.REMOTE || root_object == destination.rank) {");			
			
			output.println("\t\t\t\t\tWriteMessage w = Group.unicast[cpu_rank].newMessage();");			
			output.println("\t\t\t\t\tw.writeByte(GroupProtocol.REPLY);");			
			output.println("\t\t\t\t\tw.writeInt(ticket);");			

			if (!ret.equals(Void.TYPE)) { 
				output.println(writeMessageType("\t\t\t\t\t", "w", ret, "result"));
			}

			output.println("\t\t\t\t\tw.send();");			
			output.println("\t\t\t\t\tw.finish();");						
			output.println("\t\t\t\t}");			
	
			output.println("\t\t\t\tbreak;");
			output.println();

			output.println("\t\t\tdefault:");
			output.println("\t\t\t\tSystem.err.println(\"OOPS: group_skeleton got illegal resultMode number!\");");
			output.println("\t\t\t\tSystem.exit(1);");
			output.println("\t\t\t\tbreak;");
			output.println();

			output.println("\t\t\t}");
			output.println();

			output.println("\t\t\tbreak;");
			output.println("\t\t}");
			output.println();
		}
		

		output.println("\t\tdefault:");
		output.println("\t\t\tSystem.err.println(\"OOPS: group_skeleton got illegal method number!\");");
		output.println("\t\t\tSystem.exit(1);");
		output.println("\t\t\tbreak;");
		
		output.println("\t\t}");
		output.println("\t}");
		output.println();			       
	} 
	       
	void trailer() { 
		output.println("}\n");
	} 
       
	void constructor(Vector methods) { 

		output.println("\tpublic group_skeleton_" + data.classname + "() {");

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

			output.println("\t\tmethods[" + i + "].personalizeParameters = new Class[" + (params.length+1) + "];");
			output.println("\t\tmethods[" + i + "].personalizeParameters[0] = ibis.group.ParameterVector.class;");

			for (int j=0;j<params.length;j++) { 
				output.println("\t\tmethods[" + i + "].personalizeParameters[" + (j+1) + "] = " + getType(params[j]) + ".class;");
			}

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
			output.println();			
		}

		output.println("\t}\n");
	} 

	void generate() { 

		dest_name = data.classname;

		header();		
		constructor(data.specialMethods);
		//		constructor(data.specialMethods);		
		messageHandler(data.specialMethods);
		body(data.specialMethods);
		
		trailer();

	} 
} 

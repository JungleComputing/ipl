/* $Id$ */

package ibis.frontend.gmi;

import ibis.frontend.generic.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

class GMIStubGenerator extends GMIGenerator {

    BT_Analyzer data;

    PrintWriter output;

    GMIStubGenerator(BT_Analyzer data, PrintWriter output) {
        this.data = data;
        this.output = output;
    }

    void methodHeader(Method m) {

        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        output.print("\tpublic final " + getType(ret) + " " + m.getName()
                + "(");

        for (int j = 0; j < params.length; j++) {
            output.print(getType(params[j]) + " p" + j);

            if (j < params.length - 1) {
                output.print(", ");
            }
        }

        output.print(") {\n");

        if (!ret.equals(Type.VOID)) {
            output.println("\t\t" + getInitedLocal(ret, "result") + ";");
        }
    }

    void invokeSpecial(String spacing, Method m, Type ret, Type[] params,
            String pre) {

        if (!ret.equals(Type.VOID)) {
            output.print(spacing + "return ");
        } else {
            output.print(spacing);
        }

        output.print("GMI_" + pre + "_" + m.getName() + "(method");

        for (int j = 0; j < params.length; j++) {
            output.print(", " + " p" + j);
        }

        output.println(");");

        if (ret.equals(Type.VOID)) {
            output.print(spacing + "return;");
        }
    }

    void methodBody(String spacing, Method m, int number) {

        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        output.println(spacing + "try {");
        output.println(spacing + "\tGroupMethod method = methods[" + number
                + "];");

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

        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_FLAT_SINGLE:");
        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_FLAT_GROUP:");
        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_FLAT_PERSONAL:");
        invokeSpecial(spacing + "\t\t", m, ret, params, "COMBINED_FLAT");
        output.println();

        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_BINOMIAL_SINGLE:");
        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_BINOMIAL_GROUP:");
        output.println(spacing
                + "\tcase InvocationScheme.I_COMBINED_BINOMIAL_PERSONAL:");
        invokeSpecial(spacing + "\t\t", m, ret, params, "COMBINED_BIN");
        output.println();

        output.println(spacing + "\tdefault:");
        output.println(spacing
                + "\t\tlogger.fatal(Group.rank() + \"OOPS : group_stub got illegal "
                + "opcode\");");
        output.println(spacing + "\t\tSystem.exit(1);");
        output.println(spacing + "\t\tbreak;");
        output.println(spacing + "\t}");

        output.println(spacing + "} catch (Exception e) {");
        output.println(spacing
                + "\tlogger.fatal(Group.rank() + \"OOPS : group_stub got exception, e);");
        output.println(spacing + "\tSystem.exit(1);");
        output.println(spacing + "\t");
        output.println(spacing + "}");
    }

    void writeAdditionalData(String spacing, Type[] params) {

        output.println(spacing + "w.writeByte((byte)(method.result_mode));");
        output.println(spacing + "w.writeInt( method.index);");

        output.println(spacing + "int result_mode = method.result_mode;");
        output.println(spacing + "ReplyScheme rep = method.rep;");
        output.println(spacing
                + "if (result_mode >= ReplyScheme.R_PERSONALIZED) {");
        output.println(spacing
                + "\tresult_mode -= ReplyScheme.R_PERSONALIZED;");
        output.println(spacing
                + "\tw.writeObject(((PersonalizeReply)rep).rp);");
        output.println(spacing + "\trep = ((PersonalizeReply)rep).rs;");
        output.println(spacing + "}");

        output.println(spacing + "switch (result_mode) {");
        output.println(spacing + "case ReplyScheme.R_COMBINE_BINOMIAL:");
        output.println(spacing
                + "\tw.writeObject(((CombineReply)rep).binomialCombiner);");
        output.println(spacing + "\t// fall through");
        output.println(spacing + "case ReplyScheme.R_COMBINE_FLAT:");
        output.println(spacing + "case ReplyScheme.R_FORWARD:");
        output.println(spacing + "case ReplyScheme.R_RETURN:");
        output.println(spacing + "\tw.writeInt(Group.rank());");
        output.println(spacing + "\tticket = replyStack.get();");
        output.println(spacing + "\tw.writeInt(shiftedStubID | ticket);");
        output.println(spacing + "\tbreak;");

        output.println(spacing + "}");
        output.println();

        for (int j = 0; j < params.length; j++) {
            output.println(writeMessageType(spacing, "w", params[j], "p" + j));
        }

        output.println(spacing + "w.finish();");
    }

    void writeSpecialHeader(String spacing, Method m, String extra) {
        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        output.print(spacing + "public final " + getType(ret) + " GMI_" + extra
                + "_" + m.getName() + "(GroupMethod method");

        for (int j = 0; j < params.length; j++) {
            output.print(", " + getType(params[j]) + " p" + j);
        }

        output.print(") throws Exception {\n");
    }

    void combinedMethod(String spacing, Method m, boolean flat) {
        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();
        String caps_type = flat ? "FLAT" : "BIN";

        writeSpecialHeader(spacing, m, "COMBINED_" + caps_type);

        output.println(spacing
                + "\tif (Group.DEBUG) logger.debug(Group.rank() + \": group_stub_"
                + data.classname + "." + m.getName() + " doing COMBINED_"
                + caps_type + " call\");");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t" + getInitedLocal(ret, "result")
                    + ";");
        }

        // First, we must combine parameters.
        // The CPU with rank 0 will collect all parameters and perform the group invocation.

        output.println(spacing
                + "\tParameterVector params = new group_parameter_vector_"
                + data.classname + "_" + GMIGenerator.getUniqueName(m) + "();");

        // Fill in our own parameters.
        for (int i = 0; i < params.length; i++) {
            output.println(spacing + "\tparams.write(" + i + ", p" + i + ");");
        }

        output.println(spacing + "\tGroupMessage r = "
                + (flat ? "flat" : "bin") + "CombineInvoke(params, method);");
        output.println(spacing + "\tif (r != null) {");
        output.println(spacing + "\t\tif (r.exceptionResult != null) {");
        output.println(spacing + "\t\t\tException e = r.exceptionResult;");
        output.println(spacing + "\t\t\tfreeGroupMessage(r);");
        output.println(spacing + "\t\t\tthrow e;");
        output.println(spacing + "\t\t}");
        if (!ret.equals(Type.VOID)) {
            if (ret instanceof BasicType) {
                output.println(spacing + "\t\tresult = r." + getType(ret)
                        + "Result;");
            } else {
                output.println(spacing + "\t\tresult = (" + getType(ret)
                        + ") r.objectResult;");
            }
        }
        output.println(spacing + "\t\tfreeGroupMessage(r);");
        output.println(spacing + "\t}");
        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\treturn result;");
        }

        output.println(spacing + "}");
    }

    void singleMethod(String spacing, Method m) {

        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        writeSpecialHeader(spacing, m, "SINGLE");

        output.println(spacing
                + "\tif (Group.DEBUG) logger.debug(Group.rank() + \": group_stub_"
                + data.classname + "." + m.getName()
                + " doing SINGLE call\");");
        output.println(spacing + "\tGroupMessage r;");
        output.println(spacing + "\tint ticket = 0;");
        output.println(spacing + "\tException ex = null;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t" + getInitedLocal(ret, "result")
                    + ";");
        }

        output.println(spacing
                + "\tWriteMessage w = method.sendport.newMessage();");
        output.println(spacing + "\tw.writeByte(INVOCATION);");
        output.println(spacing + "\tw.writeInt(method.destinationSkeleton);");
        output.println(spacing
                + "\tw.writeByte((byte)(InvocationScheme.I_SINGLE));");

        writeAdditionalData(spacing + "\t", params);

        output.println(spacing
                + "\tif (result_mode == ReplyScheme.R_FORWARD) {");
        output.println(spacing + "\t\tsynchronized(this) {");
        output.println(spacing
                + "\t\t\t((ForwardReply) rep).f.startReceiving(this, "
                + "targetGroupSize, ticket);");
        output.println(spacing + "\t\t}");
        output.println(spacing + "\t} else {");
        output.println(spacing
                + "\t\tif (result_mode != ReplyScheme.R_DISCARD) {");
        output.println(spacing
                + "\t\t\tr = (GroupMessage) replyStack.collect(ticket);");
        output.println(spacing + "\t\t\tif (r.exceptionResult != null) {");
        output.println(spacing + "\t\t\t\tex = r.exceptionResult;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t\t\t} else {");
            if (ret instanceof BasicType) {
                output.println(spacing + "\t\t\t\tresult = r." + getType(ret)
                        + "Result;");
            } else {
                output.println(spacing + "\t\t\t\tresult = (" + getType(ret)
                        + ") r.objectResult;");
            }
        }
        output.println(spacing + "\t\t\t}");
        output.println(spacing + "\t\t\tfreeGroupMessage(r);");
        output.println(spacing + "\t\t}");
        output.println(spacing + "\t}");

        output.println(spacing + "\tif (ex != null) {");
        output.println(spacing + "\t\tthrow ex;");
        output.println(spacing + "\t}");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\treturn result;");
        }

        output.println(spacing + "}");
    }

    void handleGroupResult(String spacing, Type ret) {

        output.println(spacing + "switch (result_mode) {");

        output.println(spacing + "case ReplyScheme.R_COMBINE_BINOMIAL:");
        output.println(spacing + "case ReplyScheme.R_RETURN:");
        output.println(spacing
                + "\tr = (GroupMessage) replyStack.collect(ticket);");

        output.println(spacing + "\tif (r.exceptionResult != null) {");
        output.println(spacing + "\t\tex = r.exceptionResult;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t} else {");
            if (ret instanceof BasicType) {
                output.println(spacing + "\t\tresult = r." + getType(ret)
                        + "Result;");
            } else {
                output.println(spacing + "\t\tresult = (" + getType(ret)
                        + ") r.objectResult;");
            }
        }
        output.println(spacing + "\t}");
        output.println(spacing + "\tfreeGroupMessage(r);");
        output.println(spacing + "\tbreak;");

        output.println(spacing + "case ReplyScheme.R_COMBINE_FLAT:");
        if (!ret.equals(Type.VOID)) {
            if (ret instanceof BasicType) {
                output.print(spacing + "\t" + getType(ret)
                        + " [] results = new ");

                if (ret.equals(Type.BYTE)) {
                    output.println("byte");
                } else if (ret.equals(Type.CHAR)) {
                    output.println("char");
                } else if (ret.equals(Type.CHAR)) {
                    output.println("short");
                } else if (ret.equals(Type.INT)) {
                    output.println("int");
                } else if (ret.equals(Type.LONG)) {
                    output.println("long");
                } else if (ret.equals(Type.FLOAT)) {
                    output.println("float");
                } else if (ret.equals(Type.DOUBLE)) {
                    output.println("double");
                } else if (ret.equals(Type.BOOLEAN)) {
                    output.println("boolean");
                }

                output.println("[targetGroupSize];");
            } else {
                output.println(spacing
                        + "\tObject [] results = new Object[targetGroupSize];");
            }
        }

        output.println(spacing
                + "\tException [] exceptions = new "
                + "Exception[targetGroupSize];");

        output.println(spacing + "\tfor (int i=0;i<targetGroupSize;i++) {");

        output.println(spacing
                + "\t\tr = (GroupMessage) replyStack.get(ticket);");

        output.println(spacing + "\t\tif (r.exceptionResult != null) {");
        output.println(spacing
                + "\t\t\texceptions[r.rank] = r.exceptionResult;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t\t} else {");
            if (ret instanceof BasicType) {
                output.println(spacing + "\t\t\tresults[r.rank] = r."
                        + getType(ret) + "Result;");
            } else {
                output.println(spacing + "\t\t\tresults[r.rank] = ("
                        + getType(ret) + ") r.objectResult;");
            }
        }
        output.println(spacing + "\t\t}");
        output.println(spacing + "\t\tfreeGroupMessage(r);");
        output.println(spacing + "\t}");
        output.println(spacing + "\treplyStack.freeTicket(ticket);");

        if (ret instanceof BasicType) {
            if (ret.equals(Type.VOID)) {
                output.println(spacing
                        + "\t((CombineReply) rep).flatCombiner.combine("
                        + "exceptions);");
            } else {
                output.println(spacing
                        + "\tresult = ((CombineReply) rep).flatCombiner."
                        + "combine(results, exceptions);");
            }
        } else {
            output.println(spacing + "\tresult = (" + getType(ret)
                    + ") ((CombineReply) rep).flatCombiner.combine(results, "
                    + "exceptions);");
        }
        output.println(spacing + "\tbreak;");

        output.println(spacing + "case ReplyScheme.R_FORWARD:");
        output.println(spacing + "\tsynchronized(this) {");
        output.println(spacing
                + "\t\t((ForwardReply)rep).f.startReceiving(this, "
                + "targetGroupSize, ticket);");
        output.println(spacing + "\t}");
        output.println(spacing + "\tbreak;");

        output.println(spacing + "case ReplyScheme.R_DISCARD:");
        output.println(spacing + "\tbreak;");
        output.println(spacing + "}");
    }

    void groupMethod(String spacing, Method m) {

        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        writeSpecialHeader(spacing, m, "GROUP");

        output.println(spacing
                + "\tif (Group.DEBUG) logger.debug(Group.rank() + \": group_stub_"
                + data.classname + "." + m.getName() + " doing GROUP call\");");

        output.println(spacing + "\tGroupMessage r;");
        output.println(spacing + "\tint ticket = 0;");
        output.println(spacing + "\tException ex = null;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t" + getInitedLocal(ret, "result")
                    + ";");
        }

        output.println(spacing
                + "\tWriteMessage w = method.sendport.newMessage();");
        output.println(spacing + "\tw.writeByte(INVOCATION);");
        output.println(spacing + "\tw.writeInt(groupID);");
        output.println(spacing
                + "\tw.writeByte((byte)(InvocationScheme.I_GROUP));");

        writeAdditionalData(spacing + "\t", params);
        handleGroupResult(spacing + "\t", ret);

        output.println(spacing + "\tif (ex != null) {");
        output.println(spacing + "\t\tthrow ex;");
        output.println(spacing + "\t}");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\treturn result;");
        }

        output.println(spacing + "}");
    }

    void personalizedMethod(String spacing, Method m) {

        Type ret = m.getReturnType();
        Type[] params = m.getArgumentTypes();

        writeSpecialHeader(spacing, m, "PERSONAL");

        output.println(spacing
                + "\tif (Group.DEBUG) logger.debug(Group.rank() + \": group_stub_"
                + data.classname + "." + m.getName()
                + " doing PERSONAL call\");");

        output.println(spacing + "\tGroupMessage r;");
        output.println(spacing + "\tint ticket = 0;");
        output.println(spacing + "\tboolean haveTicket = false;");
        output.println(spacing + "\tException ex = null;");
        output.println(spacing + "\tWriteMessage w;");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\t" + getInitedLocal(ret, "result")
                    + ";");
        }
        
        String methodName = GMIGenerator.getUniqueName(m);

        output.println(spacing + "\tgroup_parameter_vector_" + data.classname
                + "_" + methodName + "[] pv = new group_parameter_vector_"
                + data.classname + "_" + methodName + "[targetGroupSize];");
        
        output.println(spacing + "\tgroup_parameter_vector_" + data.classname
                + "_" + methodName + " iv = new group_parameter_vector_"
                + data.classname + "_" + methodName + "();");

        for (int i = 0; i < params.length; i++) {
            output.println(spacing + "\tiv.write(" + i + ", p" + i + ");");
        }

        output.println(spacing + "\tfor (int i=0;i<targetGroupSize;i++) {");
        output.println(spacing + "\t\tpv[i] = new group_parameter_vector_"
                + data.classname + "_" + methodName + "();");
        output.println(spacing + "\t}");

        output.println(spacing + "\ttry {");
        output.println(spacing
                + "\t\t((PersonalizedInvocation)(method.inv))"
                + ".p.personalize(iv, pv);");
        output.println(spacing + "\t} catch (Exception e) {");
        output.println(spacing
                + "\t\tthrow new RuntimeException(\"OOPS: \" + e);");
        output.println(spacing + "\t}");

        output.println(spacing + "\tfor (int i=0;i<targetGroupSize;i++) {");
        output.println(spacing + "\t\tif (!pv[i].done) {");
        output.println(spacing
                + "\t\t\tthrow new RuntimeException(\"Parameters for "
                + "groupmember \" + i + \" not completed!!\");");
        output.println(spacing + "\t\t}");
        output.println(spacing + "\t}");

        output.println(spacing + "\tReplyPersonalizer personalizer = null;");
        output.println(spacing + "\tint result_mode = method.result_mode;");
        output.println(spacing + "\tReplyScheme rep = method.rep;");
        output.println(spacing
                + "\tif (result_mode >= ReplyScheme.R_PERSONALIZED) {");
        
        output.println(spacing
                + "\t\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                "group_stub_" + data.classname + "." + m.getName()
                + " reply is PERSONALIZED\");");
        
        output.println(spacing
                + "\t\tresult_mode -= ReplyScheme.R_PERSONALIZED;");
        output.println(spacing
                + "\t\tpersonalizer = ((PersonalizeReply)rep).rp;");
        output.println(spacing + "\t\trep = ((PersonalizeReply)rep).rs;");
        output.println(spacing + "\t}");

        output.println(spacing + "\tfor (int i=0;i<targetGroupSize;i++) {");
        
        output.println(spacing
                + "\t\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                "group_stub_" + data.classname + "." + m.getName()
                + " sending PERSONALIZED call to member \" + i);");
                
        output.println(spacing
                + "\t\tw = Group.unicast(memberRanks[i]).newMessage();");
        output.println(spacing + "\t\tw.writeByte(INVOCATION);");
        output.println(spacing + "\t\tw.writeInt(memberSkels[i]);");
        output.println(spacing
                + "\t\tw.writeByte((byte)(InvocationScheme.I_PERSONAL));");
        output.println(spacing
                + "\t\tw.writeByte((byte)(method.result_mode));");
        
        output.println(spacing + "\t\tw.writeInt( method.index);");
        
        output.println(spacing + "\t\tif (personalizer != null) {");
                
        output.println(spacing
                + "\t\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                "group_stub_" + data.classname + "." + m.getName()
                + " also sending personalizer to member \" + i);");
                
        output.println(spacing + "\t\t\tw.writeObject(personalizer);");
        output.println(spacing + "\t\t}");

        output.println(spacing + "\t\tswitch (result_mode) {");
        output.println(spacing + "\t\tcase ReplyScheme.R_COMBINE_BINOMIAL:");
        
        output.println(spacing
                + "\t\t\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                "group_stub_" + data.classname + "." + m.getName()
                + " also sending combiner to member \" + i);");
                
        output.println(spacing
                + "\t\t\tw.writeObject(((CombineReply)rep).binomialCombiner);");
        output.println(spacing + "\t\t\t// fall through");
        output.println(spacing + "\t\tcase ReplyScheme.R_COMBINE_FLAT:");
        output.println(spacing + "\t\tcase ReplyScheme.R_FORWARD:");
        output.println(spacing + "\t\tcase ReplyScheme.R_RETURN:");
        
        output.println(spacing
                + "\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                "group_stub_" + data.classname + "." + m.getName()
                + " also sending reply rank and ticket to member \" + i);");
                
        output.println(spacing + "\t\t\tw.writeInt(Group.rank());");
        output.println(spacing + "\t\t\tif (!haveTicket) {");
        output.println(spacing + "\t\t\t\tticket = replyStack.get();");
        output.println(spacing + "\t\t\t\thaveTicket = true;");
        output.println(spacing + "\t\t\t}");
        output.println(spacing + "\t\t\tw.writeInt(shiftedStubID | ticket);");
        output.println(spacing + "\t\t\tbreak;");
        output.println(spacing + "\t\t}");
        output.println();

        for (int j = 0; j < params.length; j++) {
            
            output.println(spacing
                    + "\t\tif (Group.DEBUG) logger.debug(Group.rank() + \": " + 
                    "group_stub_" + data.classname + "." + m.getName()
                    + " sending " + params[j] + " parameter (\" + pv[i].p" + j +
                    "+ \") to member \" + i);");
                        
            if (params[j] instanceof ArrayType) {
                output.println(spacing + "\t\tif (pv[i].p" + j
                        + "_subarray) {");
                output.println(spacing + "\t\t\tw.writeArray(pv[i].p" + j
                        + ", pv[i].p" + j + "_offset, pv[i].p" + j + "_size);");
                output.println(spacing + "\t\t}else {");
                output.println(writeMessageType(spacing + "\t\t\t", "w",
                        params[j], "pv[i].p" + j));
                output.println(spacing + "\t\t}");
            } else {
                output.println(writeMessageType(spacing + "\t\t", "w",
                        params[j], "pv[i].p" + j));
            }
        }
        output.println(spacing + "\t\tw.finish();");
        output.println(spacing + "\t}");

        handleGroupResult(spacing + "\t", ret);

        output.println(spacing + "\tif (ex != null) {");
        output.println(spacing + "\t\tthrow ex;");
        output.println(spacing + "\t}");

        if (!ret.equals(Type.VOID)) {
            output.println(spacing + "\treturn result;");
        }

        output.println(spacing + "}");
    }

    void methodTrailer(Method m) {

        Type ret = m.getReturnType();

        if (!ret.equals(Type.VOID)) {
            output.println("\t\treturn result;");
        }

        output.println("\t}\n");
    }

    void header() {

        //Type [] interfaces = data.subject.getInterfaces();

        if (data.packagename != null && !data.packagename.equals("")) {
            output.println("package " + data.packagename + ";");
            output.println();
        }

        output.println("import ibis.gmi.*;");
        output.println("import ibis.ipl.*;");
        output.println();

        output.print("public final class group_stub_" + data.classname
                + " extends ibis.gmi.GroupStub implements ");
        output.print(data.subject.getClassName());

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

        for (int i = 0; i < methods.size(); i++) {

            Method m = (Method) methods.get(i);

            Type ret = m.getReturnType();
            Type[] params = m.getArgumentTypes();

            output.print("\t\tmethods[" + i + "] = new GroupMethod(this, " + i
                    + ", new group_parameter_vector_" + data.classname + "_"
                    + GMIGenerator.getUniqueName(m) + "(), \"");
            output.print(getType(ret) + " " + m.getName() + "(");
            for (int j = 0; j < params.length; j++) {
                output.print(getType(params[j]));

                if (j < params.length - 1) {
                    output.print(", ");
                }
            }

            output.println(")\");");

            output.println("\t\tmethods[" + i
                    + "].invocation_mode = "
                    + "ibis.gmi.InvocationScheme.I_SINGLE;");
            output.println("\t\tmethods[" + i
                    + "].result_mode = ibis.gmi.ReplyScheme.R_RETURN;");

            output.println("\t\tmethods[" + i + "].destinationSkeleton = -1;");
            output.println();
        }

        output.println("\t}\n");
    }

    void body(Vector methods) {

        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);

            methodHeader(m);
            methodBody("\t\t", m, i);
            methodTrailer(m);

            singleMethod("\t", m);
            groupMethod("\t", m);
            personalizedMethod("\t", m);
            combinedMethod("\t", m, true);
            combinedMethod("\t", m, false);
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

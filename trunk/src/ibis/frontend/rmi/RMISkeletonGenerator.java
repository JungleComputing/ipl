/* $Id$ */

package ibis.frontend.rmi;

import ibis.frontend.generic.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.Type;

class RMISkeletonGenerator extends RMIGenerator {

    BT_Analyzer data;

    PrintWriter output;

    String dest_name;

    RMISkeletonGenerator(BT_Analyzer data, PrintWriter output) {
        this.data = data;
        this.output = output;
    }

    void header(Vector methods) {
        if (data.packagename != null && !data.packagename.equals("")) {
            output.println("package " + data.packagename + ";");
            output.println();
        }

        output.println("import ibis.rmi.*;");
        output.println("import ibis.rmi.impl.RTS;");
        output.println("import java.lang.reflect.*;");
        output.println("import ibis.ipl.*;");
        output.println("import java.io.IOException;");
        output.println("import colobus.Colobus;");
        output.println();

        output.println("public final class rmi_skeleton_" + dest_name
                + " extends ibis.rmi.impl.Skeleton {");
        output.println();
        for (int i = 0; i < methods.size(); i++) {
            output.println("\tprivate ibis.util.Timer timer_" + i + ";");
        }
        output.println();

        output.println("\tprivate static final Colobus colobus = Colobus.getColobus(rmi_skeleton_" + dest_name + ".class.getName());");
        output.println();
    }

    private boolean simpleMethod(Method m) {
        Code c = m.getCode();

        if (c == null) {
            return true;
        }

        InstructionList il = new InstructionList(c.getCode());

        Instruction[] ins = il.getInstructions();

        int len = 0;

        for (int i = 0; i < ins.length; i++) {
            if (ins[i] instanceof InvokeInstruction) {
                return false;
            }
            if (ins[i] instanceof MONITORENTER) {
                return false;
            }
            if (ins[i] instanceof BranchInstruction) {
                BranchInstruction ib = (BranchInstruction) ins[i];
                if (ib.getIndex() <= len) {
                    // A jump backwards. Assume loop.
                    return false;
                }
            }
            len += ins[i].getLength();
        }
        return true;
    }

    static boolean throwsRemote(Method m) {

        ExceptionTable e = m.getExceptionTable();
        if (e == null) {
            return false;
        }

        String names[] = e.getExceptionNames();

        for (int i = 0; i < names.length; i++) {
            if (names[i].equals("ibis.rmi.RemoteException")) {
                return true;
            }
        }
        return false;
    }

    void messageHandler(Vector methods) {

        output.println("\tpublic final void upcall(ReadMessage r, int method, "
                + "int stubID) throws ibis.rmi.RemoteException {");
        output.println();
        output.println();

        //gosia
        // Do not use IP addresses, a machine may have multiple, and
        // Ibis does not know about IP anymore --Rob
        output.println("\t\tRTS.setClientHost(r.origin().ibis().toString());");
        //end gosia

        output.println("\t\tException ex = null;");
        output.println();

        output.println("\t\tswitch(method) {");

        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            Type ret = getReturnType(m);
            Type[] params = getParameterTypes(m);
            boolean has_object_params = false;
            boolean is_simple_method = simpleMethod(m);

            output.println("\t\tcase " + i + ": /* method " + m.getName() + " */");
            output.println("\t\t{");

            output.println("\t\t\tlong handle = colobus.fireStartEvent(\"RMI parameter deserialization of method " + m.getName() + "\");");

            output.println("\t\t\tRTS.startRMITimer(timer_" + i + ");");

            output.println("\t\t\t/* First - Extract the parameters */");

            if (params.length > 0 || !is_simple_method) {
                for (int j = 0; j < params.length; j++) {
                    Type temp = params[j];
                    output.println("\t\t\t" + temp + " p" + j + ";");
                    if (!(temp instanceof BasicType)) {
                        has_object_params = true;
                    }
                }
                output.println("\t\t\ttry {");
                for (int k = 0; k < params.length; k++) {
                    Type temp = params[k];
                    output.println(readMessageType("\t\t\t\t", "p" + k, "r",
                            temp));
                }
                // we should try to optimize this finish away, to avoid 
                // thread creation!!! --Rob
                // We can do this if
                // - the method does not have loops
                // - the method does not do any other method invocations.
                // - no synchronization
                // (Ceriel)
                output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI parameter deserialization of method " + m.getName() + "\");");

                if (!is_simple_method) {
                    output.println("\t\t\t\tr.finish();");
                }
                if (has_object_params) {
                    output.println("\t\t\t} catch(ClassNotFoundException e) {");
                    output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI parameter deserialization of method " + m.getName() + "\");");
                    output.println("\t\t\t\tthrow new "
                            + "ibis.rmi.UnmarshalException("
                            + "\"error unmarshalling arguments\", e);");
                }
                output.println("\t\t\t} catch(IOException e) {");
                output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI parameter deserialization of method " + m.getName() + "\");");
                output.println("\t\t\t\tthrow new "
                        + "ibis.rmi.UnmarshalException("
                        + "\"error unmarshalling arguments\", e);");
                output.println("\t\t\t}");
            } else {
                output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI parameter deserialization of method " + m.getName() + "\");");
            }
            
            output.println();

            output.println("\t\t\thandle = colobus.fireStartEvent(\"RMI user method invocation of method " + m.getName() + "\");");

            output.println("\t\t\t/* Second - Invoke the method */");

            if (!ret.equals(Type.VOID)) {
                output.println("\t\t\t" + getInitedLocal(ret, "result") + ";");
            }

            output.println("\t\t\ttry {");
            output.print("\t\t\t\t");

            if (!ret.equals(Type.VOID)) {
                output.print("result = ");
            }

            output.print("((" + dest_name + ") destination)." + m.getName()
                    + "(");

            for (int j = 0; j < params.length; j++) {
                output.print("p" + j);

                if (j < params.length - 1) {
                    output.print(", ");
                }
            }
            output.println(");");
            if (throwsRemote(m)) {
                output.println("\t\t\t} catch (ibis.rmi.RemoteException e) {");
                output.println("\t\t\t\tex = new ibis.rmi.ServerException("
                        + "\"server exception\", e);");
            }
            output.println("\t\t\t} catch (RuntimeException e) {");
            output.println("\t\t\t\tex = new ibis.rmi.ServerRuntimeException("
                    + "\"server runtime exception\", e);");
            output.println("\t\t\t} catch (Error e) {");
            output.println("\t\t\t\tex = new ibis.rmi.ServerError("
                    + "\"server error\", e);");
            output.println("\t\t\t} catch (Exception e) {");
            output.println("\t\t\t\tex = e;");
            output.println("\t\t\t}");

            output.println("\t\t\tRTS.stopRMITimer(timer_" + i + ");");

            output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI user method invocation of method " + m.getName() + "\");");

            output.println("\t\t\thandle = colobus.fireStartEvent(\"RMI reply message of method " + m.getName() + "\");");

            output.println("\t\t\tWriteMessage w;");
            output.println("\t\t\ttry {");
            output.println("\t\t\t\tw = stubs[stubID].newMessage();");
            output.println("\t\t\t} catch(IOException e) {");
            output.println("\t\t\t\tcolobus.fireStopEvent(handle, \"RMI reply message of method " + m.getName() + "\");");
            output.println("\t\t\t\tthrow new ibis.rmi.MarshalException("
                    + "\"error marshalling return\", e);");
            output.println("\t\t\t}");

            output.println("\t\t\ttry {");
            output.println("\t\t\t\tif (ex != null) {");
            output.println("\t\t\t\t\tw.writeByte(RTS.EXCEPTION);");
            output.println("\t\t\t\t\tw.writeObject(ex);");
            output.println("\t\t\t\t} else {");
            output.println("\t\t\t\t\tw.writeByte(RTS.RESULT);");

            if (!ret.equals(Type.VOID)) {
                output.println(writeMessageType("\t\t\t\t\t", "w", ret,
                        "result"));
            }

            output.println("\t\t\t\t}");
            output.println("\t\t\t\tw.finish();");
            output.println("\t\t\t} catch(IOException e) {");
            output.println("\t\t\t\tw.finish(e);");
            output.println("\t\t\t\tcolobus.fireStopEvent(handle, \"RMI reply message of method " + m.getName() + "\");");
            output.println("\t\t\t\tthrow new ibis.rmi.MarshalException("
                    + "\"error marshalling return\", e);");
            output.println("\t\t\t}");
            output.println("\t\t\tcolobus.fireStopEvent(handle, \"RMI reply message of method " + m.getName() + "\");");

            output.println("\t\t\tbreak;");
            output.println("\t\t}");
            output.println();
        }

        output.println("\t\tcase -1:");
        output.println("\t\t{");
        output.println("\t\t\t/* Special case: new stub connecting */");
        output.println("\t\t\tReceivePortIdentifier rpi;");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\trpi = (ReceivePortIdentifier) r.readObject();");
        output.println("\t\t\t\tr.finish();");
        output.println("\t\t\t} catch(ClassNotFoundException e) {");
        output.println("\t\t\t\tthrow new ibis.rmi.UnmarshalException("
                + "\"while reading ReceivePortIdentifier\", e);");
        output.println("\t\t\t} catch(IOException e) {");
        output.println("\t\t\t\tr.finish(e);");
        output.println("\t\t\t\tthrow new ibis.rmi.UnmarshalException("
                + "\"while reading ReceivePortIdentifier\", e);");
        output.println("\t\t\t}");
        output.println("\t\t\tint id = addStub(rpi);");
        output.println("\t\t\tWriteMessage w;");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tw = stubs[id].newMessage();");
        output.println("\t\t\t} catch(IOException e) {");
        output.println("\t\t\t\tthrow new ibis.rmi.MarshalException("
                + "\"error sending skeletonId\", e);");
        output.println("\t\t\t}");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tw.writeInt(id);");
        output.println("\t\t\t\tw.writeObject(destination);");
        output.println("\t\t\t\tw.finish();");
        output.println("\t\t\t} catch(IOException e) {");
        output.println("\t\t\t\tw.finish(e);");
        output.println("\t\t\t\tthrow new ibis.rmi.MarshalException("
                + "\"error sending skeletonId\", e);");
        output.println("\t\t\t}");
        output.println("\t\t\tbreak;");
        output.println("\t\t}");
        output.println();

        output.println("\t\tdefault:");
        output.println("\t\t\tthrow new ibis.rmi.UnmarshalException("
                + "\"invalid method number\");");

        output.println("\t\t}");

        output.println();

        output.println("\t}");
        output.println();
    }

    void trailer() {
        output.println("}");
        output.println();
    }

    void constructor(Vector methods) {

        output.println("\tpublic rmi_skeleton_" + data.classname + "() {");

        //		output.println("\t\tsuper();");
        if (data.packagename != null && !data.packagename.equals("")) {
            output.println("\t\tstubType = \"" + data.packagename
                    + ".rmi_stub_" + data.classname + "\";");
        } else {
            output.println("\t\tstubType = \"rmi_stub_" + data.classname
                    + "\";");
        }
        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            output.println("\t\ttimer_" + i
                    + " = RTS.createRMITimer(this.toString() + \"_"
                    + m.getName() + "_\" + " + i + ");");
        }
        output.println("\t}");
        output.println();
    }

    void generate() {

        dest_name = data.classname;

        header(data.subjectSpecialMethods);
        constructor(data.subjectSpecialMethods);
        messageHandler(data.subjectSpecialMethods);
        trailer();
    }
}

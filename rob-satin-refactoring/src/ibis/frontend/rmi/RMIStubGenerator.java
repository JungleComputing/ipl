/* $Id$ */

package ibis.frontend.rmi;

import ibis.frontend.generic.BT_Analyzer;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

class RMIStubGenerator extends RMIGenerator {

    BT_Analyzer data;

    PrintWriter output;

    RMIStubGenerator(BT_Analyzer data, PrintWriter output) {
        this.data = data;
        this.output = output;
    }

    void methodHeader(Method m) {

        Type ret = getReturnType(m);
        Type[] params = getParameterTypes(m);

        output.print("\tpublic final " + ret + " " + m.getName() + "(");

        for (int j = 0; j < params.length; j++) {
            output.print(params[j] + " p" + j);

            if (j < params.length - 1) {
                output.print(", ");
            }
        }
        ExceptionTable et = m.getExceptionTable();
        output.print(") throws ");
        if (et != null) {
            String[] names = et.getExceptionNames();
            for (int i = 0; i < names.length; i++) {
                output.print(names[i]);
                if (i < names.length - 1) {
                    output.print(", ");
                }
            }
        } else {
            output.print("RemoteException");
        }

        output.print(" {");
        output.println();
    }

    private boolean caught(ExceptionTable t, String name) {
        String[] names = t.getExceptionNames();
        for (int i = 0; i < names.length; i++) {
            if (Repository.instanceOf(name, names[i])) {
                return true;
            }
        }
        return false;
    }

    void methodBody(Method m, int number) {
        Type ret = getReturnType(m);
        Type[] params = getParameterTypes(m);

        if (!ret.equals(Type.VOID)) {
            output.println("\t\t" + getInitedLocal(ret, "result") + ";");
        }

        output.println("\t\ttry {");
        output.println("\t\t\tException remoteex = null;");
        output.println("\t\t\tWriteMessage w;");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tinitSend();");
        output.println("\t\t\t\tw = newMessage();");
        output.println("\t\t\t} catch(java.io.IOException ioex) {");
        output.println("\t\t\t\tthrow new RemoteException(\"IO exception\","
                + " ioex);");
        output.println("\t\t\t}");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tw.writeInt(" + number + ");");
        output.println("\t\t\t\tw.writeInt(stubID);");

        for (int j = 0; j < params.length; j++) {
            output.println(
                    writeMessageType("\t\t\t\t", "w", params[j], "p" + j));
        }

        output.println("\t\t\t\tRTS.startRMITimer(timer_" + number + ");");
        output.println("\t\t\t\tw.finish();");
        output.println("\t\t\t} catch(java.io.IOException ioex) {");
        output.println("\t\t\t\tw.finish(ioex);");
        output.println("\t\t\t\tthrow new RemoteException(\"IO exception\","
                + " ioex);");
        output.println("\t\t\t}");

        output.println("\t\t\tReadMessage r;");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tr = reply.receive();");
        output.println("\t\t\t} catch(java.io.IOException ioex) {");
        output.println("\t\t\t\tthrow new RemoteException(\"IO exception\","
                + " ioex);");
        output.println("\t\t\t}");
        output.println("\t\t\tRTS.stopRMITimer(timer_" + number + ");");
        output.println("\t\t\ttry {");
        output.println("\t\t\t\tif (r.readByte() == RTS.EXCEPTION) {");
        output.println("\t\t\t\t\tremoteex = (Exception) r.readObject();");
        output.println("\t\t\t\t}");
        output.println("\t\t\t\telse {");
        if (!ret.equals(Type.VOID)) {
            output.println(readMessageType("\t\t\t\t\t", "result", "r", ret));
        }
        output.println("\t\t\t\t}");
        output.println("\t\t\t\tr.finish();");
        output.println("\t\t\t} catch(java.io.IOException ioex) {");
        output.println("\t\t\t\tr.finish(ioex);");
        output.println("\t\t\t\tthrow new RemoteException(\"IO exception\","
                + " ioex);");
        output.println("\t\t\t}");
        output.println("\t\t\tif (remoteex != null) throw remoteex;");
        ExceptionTable et = m.getExceptionTable();
        if (et != null) {
            String[] names = et.getExceptionNames();

            /* If the method throws E1 and E2 and E2 is a superclass of E1,
             * this will yield a compiler error:
             * } catch (E2 e2) {
             * ....
             * } catch (E1 e1) {
             * ....
             * because e1 has already been caught under the E2 clause.
             * Solution: traverse the hierarchy for the exceptions, and
             * disable catching for all E1s that have a superclass that
             * is also caught.
             *						RFHH
             */
            JavaClass[] excpt = new JavaClass[names.length];
            for (int i = 0, n = names.length; i < n; i++) {
                excpt[i] = Repository.lookupClass(names[i]);
            }
            boolean[] disable = new boolean[names.length];
            for (int i = 0, n = names.length; i < n; i++) {
                disable[i] = false;
                JavaClass[] supers = excpt[i].getSuperClasses();
                outer: for (int s = 0; s < supers.length; s++) {
                    for (int j = 0; j < n; j++) {
                        if (excpt[j].equals(supers[s])) {
                            disable[i] = true;
                            break outer;
                        }
                    }
                }
            }

            for (int i = 0; i < names.length; i++) {
                if (!disable[i]) {
                    output.println("\t\t} catch (" + names[i] + " e" + i
                            + ") {");
                    output.println("\t\t\tthrow e" + i + ";");
                }
            }
        } else {
            output.println("\t\t} catch (RemoteException e0) {");
            output.println("\t\t\tthrow e0;");
        }
        if (et == null || !(caught(et, "java.lang.RuntimeException"))) {
            output.println("\t\t} catch (RuntimeException re) {");
            output.println("\t\t\tthrow re;");
        }
        if (et == null || !(caught(et, "java.lang.Exception"))) {
            output.println("\t\t} catch (Exception e) {");
            output.println("\t\t\tthrow new RemoteException("
                    + "\"undeclared checked exception\", e);");
        }
        output.println("\t\t}");
    }

    void methodTrailer(Method m) {

        Type ret = getReturnType(m);

        if (!ret.equals(Type.VOID)) {
            output.println("\t\treturn result;");
        }
        output.println("\t}");
        output.println();
    }

    void header(Vector methods) {

        Vector interfaces = data.specialInterfaces;

        if (data.packagename != null && !data.packagename.equals("")) {
            output.println("package " + data.packagename + ";");
            output.println();
        }

        output.println("import ibis.rmi.*;");
        output.println("import ibis.rmi.impl.RTS;");
        output.println("import ibis.ipl.*;");
        output.println();

        output.print("public final class rmi_stub_" + data.classname
                + " extends ibis.rmi.impl.Stub implements ");

        for (int i = 0; i < interfaces.size(); i++) {
            output.print(((JavaClass) interfaces.get(i)).getClassName());

            if (i < interfaces.size() - 1) {
                output.print(", ");
            }
        }

        output.println(" {");
        output.println();
        for (int i = 0; i < methods.size(); i++) {
            output.println("\tprivate ibis.util.Timer timer_" + i + ";");
        }
        output.println();
    }

    void constructor(Vector methods) {
        output.println("\tpublic rmi_stub_" + data.classname + "() {");
        // output.println("\t\tsuper();");
        // output.println("\t}");
        // output.println();
        // output.println("\tprotected void initStubDatastructures() {");
        // output.println("\t\tsuper();");
        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            output.println("\t\ttimer_" + i
                    + " = RTS.createRMITimer(this.toString() + \"_"
                    + m.getName() + "_\" + " + i + ");");
        }
        output.println("\t}");
        output.println();
    }

    void body(Vector methods) {

        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            methodHeader(m);
            methodBody(m, i);
            methodTrailer(m);
        }
    }

    void trailer() {
        output.println("}");
        output.println();
    }

    void generate() {
        header(data.specialMethods);
        constructor(data.specialMethods);
        body(data.specialMethods);
        trailer();
    }

}


/* $Id$ */

package ibis.frontend.gmi;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

class GMIGenerator {

    public static String getInitedLocal(Type c, String name) {

        String result = null;

        if (!c.equals(Type.VOID)) {
            if (c instanceof BasicType) {
                if (c.equals(Type.BYTE)) {
                    result = "byte " + name + " = 0";
                } else if (c.equals(Type.CHAR)) {
                    result = "char " + name + " = 0";
                } else if (c.equals(Type.SHORT)) {
                    result = "short " + name + " = 0";
                } else if (c.equals(Type.INT)) {
                    result = "int " + name + " = 0";
                } else if (c.equals(Type.LONG)) {
                    result = "long " + name + " = 0";
                } else if (c.equals(Type.FLOAT)) {
                    result = "float " + name + " = 0.0";
                } else if (c.equals(Type.DOUBLE)) {
                    result = "double " + name + " = 0.0";
                } else if (c.equals(Type.BOOLEAN)) {
                    result = "boolean " + name + " = false";
                }
            } else {
                result = getType(c) + " " + name + " = null";
            }
        }

        return result;
    }

    public static String containerType(Type c) {

        if (!c.equals(Type.VOID)) {

            if (c instanceof BasicType) {

                if (c.equals(Type.BYTE)) {
                    return "Byte";
                } else if (c.equals(Type.CHAR)) {
                    return "Character";
                } else if (c.equals(Type.SHORT)) {
                    return "Short";
                } else if (c.equals(Type.INT)) {
                    return "Integer";
                } else if (c.equals(Type.LONG)) {
                    return "Long";
                } else if (c.equals(Type.FLOAT)) {
                    return "Float";
                } else if (c.equals(Type.DOUBLE)) {
                    return "Double";
                } else if (c.equals(Type.BOOLEAN)) {
                    return "Boolean";
                }
            }
            return "Object";
        }
        return "Object";
    }

    public static String getType(Type c) {

        if (c instanceof ArrayType) {
            return getType(((ArrayType) c).getElementType()) + "[]";
        }
        if (!c.equals(Type.VOID)) {

            if (c instanceof BasicType) {

                if (c.equals(Type.BYTE)) {
                    return "byte";
                } else if (c.equals(Type.CHAR)) {
                    return "char";
                } else if (c.equals(Type.SHORT)) {
                    return "short";
                } else if (c.equals(Type.INT)) {
                    return "int";
                } else if (c.equals(Type.LONG)) {
                    return "long";
                } else if (c.equals(Type.FLOAT)) {
                    return "float";
                } else if (c.equals(Type.DOUBLE)) {
                    return "double";
                } else if (c.equals(Type.BOOLEAN)) {
                    return "boolean";
                }
            }
            return c.toString();
        }
        return "void";
    }

    public static String getArrayType(ArrayType c, String s) {
        Type e = c.getElementType();
        if (e instanceof ArrayType) {
            return getArrayType((ArrayType) e, s) + "[]";
        }
        return getType(e) + "[" + s + "]";
    }

    public static String printType(Type c) {

        if (!c.equals(Type.VOID)) {

            if (c instanceof BasicType) {

                if (c.equals(Type.BYTE)) {
                    return "Byte";
                } else if (c.equals(Type.CHAR)) {
                    return "Char";
                } else if (c.equals(Type.SHORT)) {
                    return "Short";
                } else if (c.equals(Type.INT)) {
                    return "Int";
                } else if (c.equals(Type.LONG)) {
                    return "Long";
                } else if (c.equals(Type.FLOAT)) {
                    return "Float";
                } else if (c.equals(Type.DOUBLE)) {
                    return "Double";
                } else if (c.equals(Type.BOOLEAN)) {
                    return "Boolean";
                }
            } else {
                return "Object";
            }
        }
        return null;
    }

    public static String writeMessageType(String pre, String message, Type c,
            String param) {
        return (pre + message + ".write" + printType(c) + "(" + param + ");");
    }

    public static String readMessageType(String pre, String dest,
            String message, Type c, boolean cast) {
        String temp = pre + dest + " = ";

        if (!(c instanceof BasicType) && cast) {
            temp += "(" + getType(c) + ") ";
        }

        temp += message + ".read" + printType(c) + "();";

        return temp;
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

    public static String getUniqueName(Method m) {                 
        return do_mangle(m.getName(), m.getSignature());
    }
    
    public static String getUniqueMethodName(Method m) {
        return "GMI_" + getUniqueName(m);
    }
}

package ibis.impl.net;

import java.util.TreeMap;

public class NetMessageStat extends NetStat {
    private int nb_msg = 0;

    private int nb_boolean = 0;

    private int nb_byte = 0;

    private int nb_char = 0;

    private int nb_short = 0;

    private int nb_int = 0;

    private int nb_long = 0;

    private int nb_float = 0;

    private int nb_double = 0;

    private int nb_string = 0;

    private int nb_object = 0;

    private TreeMap booleanArrayMap = null;

    private TreeMap byteArrayMap = null;

    private TreeMap charArrayMap = null;

    private TreeMap shortArrayMap = null;

    private TreeMap intArrayMap = null;

    private TreeMap longArrayMap = null;

    private TreeMap floatArrayMap = null;

    private TreeMap doubleArrayMap = null;

    private TreeMap objectArrayMap = null;

    private TreeMap bufferMap = null;

    public NetMessageStat(boolean on, String moduleName) {
        super(on, moduleName);

        if (on) {
            booleanArrayMap = new TreeMap();
            byteArrayMap = new TreeMap();
            charArrayMap = new TreeMap();
            shortArrayMap = new TreeMap();
            intArrayMap = new TreeMap();
            longArrayMap = new TreeMap();
            floatArrayMap = new TreeMap();
            doubleArrayMap = new TreeMap();
            objectArrayMap = new TreeMap();

            bufferMap = new TreeMap();
        }
    }

    public NetMessageStat(boolean on) {
        this(on, "");
    }

    public void begin() {
        if (on) {
            nb_msg++;
        }
    }

    public void addBoolean() {
        if (on) {
            nb_boolean++;
        }
    }

    public void addByte() {
        if (on) {
            nb_byte++;
        }
    }

    public void addChar() {
        if (on) {
            nb_char++;
        }
    }

    public void addShort() {
        if (on) {
            nb_short++;
        }
    }

    public void addInt() {
        if (on) {
            nb_int++;
        }
    }

    public void addLong() {
        if (on) {
            nb_long++;
        }
    }

    public void addFloat() {
        if (on) {
            nb_float++;
        }
    }

    public void addDouble() {
        if (on) {
            nb_double++;
        }
    }

    public void addString() {
        if (on) {
            nb_string++;
        }
    }

    public void addObject() {
        if (on) {
            nb_object++;
        }
    }

    public void addBooleanArray(int l) {
        if (on) {
            Integer n = (Integer) booleanArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            booleanArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addByteArray(int l) {
        if (on) {
            Integer n = (Integer) byteArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            byteArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addCharArray(int l) {
        if (on) {
            Integer n = (Integer) charArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            charArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addShortArray(int l) {
        if (on) {
            Integer n = (Integer) shortArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            shortArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addIntArray(int l) {
        if (on) {
            Integer n = (Integer) intArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            intArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addLongArray(int l) {
        if (on) {
            Integer n = (Integer) longArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            longArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addFloatArray(int l) {
        if (on) {
            Integer n = (Integer) floatArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            floatArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addDoubleArray(int l) {
        if (on) {
            Integer n = (Integer) doubleArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            doubleArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addObjectArray(int l) {
        if (on) {
            Integer n = (Integer) objectArrayMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            objectArrayMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void addBuffer(int l) {
        if (on) {
            Integer n = (Integer) bufferMap.get(new Integer(l));
            if (n == null) {
                n = new Integer(0);
            }
            bufferMap.put(new Integer(l), new Integer(n.intValue() + 1));
        }
    }

    public void end() {
        if (on) {
            //
        }
    }

    public void report() {
        if (on) {
            System.err.println();
            System.err.println("Message stats for module " + moduleName);
            System.err.println("------------------------------------");
            reportVal(nb_msg, " message");

            reportVal(nb_boolean, " boolean");
            reportVal(nb_byte, " byte");
            reportVal(nb_char, " char");
            reportVal(nb_short, " short");
            reportVal(nb_int, " int");
            reportVal(nb_long, " long");
            reportVal(nb_float, " float");
            reportVal(nb_double, " double");
            reportVal(nb_string, " string");
            reportVal(nb_object, " object");

            reportMap(booleanArrayMap, "array", "boolean");
            reportMap(byteArrayMap, "array", "byte");
            reportMap(charArrayMap, "array", "char");
            reportMap(shortArrayMap, "array", "short");
            reportMap(intArrayMap, "array", "int");
            reportMap(longArrayMap, "array", "long");
            reportMap(floatArrayMap, "array", "float");
            reportMap(doubleArrayMap, "array", "double");
            reportMap(objectArrayMap, "array", "object");
            reportMap(bufferMap, "buffer", "byte");
            System.err.println("____________________________________");
        }
    }
}
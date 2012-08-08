package ibis.ipl.impl.stacking.cc.util;

import java.util.logging.Logger;

public class Loggers {

    public static final Logger ccLog;
    public static final Logger conLog;
//    public static final String conLogString;
    public static final Logger upcallLog;
//    public static final String upcallLogString;
    public static final Logger readMsgLog;
//    public static final String readMsgLogString;
    public static final Logger writeMsgLog;
//    public static final String writeMsgLogString;
    public static final Logger sideLog;
//    public static final String sideLogString;
    public static final Logger lockLog;
    public static final Logger statsLog;

    static {
        ccLog = Logger.getLogger("connCache");

        conLog = Logger.getLogger("con");

        upcallLog = Logger.getLogger("upcall");

        readMsgLog = Logger.getLogger("read");

        writeMsgLog = Logger.getLogger("write");

        sideLog = Logger.getLogger("side");
        
        lockLog = Logger.getLogger("lock");
        
        statsLog = Logger.getLogger("stats");
    }   
}

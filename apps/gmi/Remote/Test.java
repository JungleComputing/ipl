/* $Id$ */


import ibis.gmi.GroupMember;
import ibis.util.GetLogger;

import org.apache.log4j.Logger;

class Test extends GroupMember implements myGroup {

    int i;

    Object data;

    static Logger logger = GetLogger.getLogger(Test.class.getName());

    Test() {
        logger.debug(getRank() + ": Test()");
    }

    public void groupInit() {
        i = getRank();
        logger.debug(getRank() + ": Test.groupInit()");
    }

    public void put(Object o) {
        logger.debug(getRank() + ": Test.put()");
        data = o;
    }

    public Object get() {
        logger.debug(getRank() + ": Test.get()");
        return data;
    }

    public Object put_get(Object o) {
        logger.debug(getRank() + ": Test.put_get()");
        Main.inc_count();
        return o;
    }
}

/* $Id$ */


import ibis.gmi.GroupMember;

import org.apache.log4j.Logger;

class Test extends GroupMember implements myGroup {

    static Logger logger = Logger.getLogger(Test.class.getName());

    int i;

    Test() {
        logger.debug(myGroupRank + ": Test()");
    }

    public void groupInit() {
        i = myGroupRank;
        logger.debug(myGroupRank + ": Test.groupInit()");
    }

    public void put(int i) {
        logger.debug(myGroupRank + ": Test.put(" + i + ")");
        this.i = i;
    }

    public int get() {
        logger.debug(myGroupRank + ": Test.get() = " + i);
        return i;
    }
}

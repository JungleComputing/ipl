/* $Id$ */

package ibis.gmi;

/** Container class for a registry reply. */
class RegistryReply implements java.io.Serializable {
    byte result;

    int groupnum = 0;

    String str = null;

    int[] memberRanks = null;

    int[] memberSkels = null;

    CombinedInvocationInfo inf = null;

    RegistryReply(byte r) {
        result = r;
    }

    RegistryReply(byte r, int num, int[] ranks, int[] skels) {
        this(r);
        groupnum = num;
        memberRanks = ranks;
        memberSkels = skels;
    }

    RegistryReply(byte r, String type, int num, int[] ranks, int[] skels) {
        this(r, num, ranks, skels);
        this.str = type;
    }

    RegistryReply(byte r, String reason) {
        this(r);
        str = reason;
    }

    RegistryReply(byte r, CombinedInvocationInfo inf) {
        this(r);
        this.inf = inf;
    }
}

package ibis.impl.messagePassing;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by ibis.impl.messagePassing.
 */
class MPProps {
    static final String PROPERTY_PREFIX = "ibis.mp.";

    static final String s_polls_optim = PROPERTY_PREFIX + "polls.optimistic";

    static final String s_polls_yield = PROPERTY_PREFIX + "polls.yield";

    static final String s_intr_dis_multi = PROPERTY_PREFIX
            + "intr.disable.multifragment";

    static final String s_election = PROPERTY_PREFIX + "election";

    static final String s_elect_debug = PROPERTY_PREFIX + "elect.debug";

    static final String s_debug = PROPERTY_PREFIX + "debug";

    static final String s_yield = PROPERTY_PREFIX + "yield";

    static final String s_broadcast = PROPERTY_PREFIX + "broadcast";

    static final String s_bc_native = PROPERTY_PREFIX + "broadcast.native";

    static final String s_bc_all = PROPERTY_PREFIX + "broadcast.all";

    static final String s_bc_2 = PROPERTY_PREFIX + "broadcast.2";

    static final String s_ser_sends = PROPERTY_PREFIX + "serialize-sends";

    static final String s_ns_debug = PROPERTY_PREFIX + "ns.debug";

    private static final String[] sysprops = { s_polls_optim, s_polls_yield,
            s_intr_dis_multi, s_election, s_elect_debug, s_debug, s_yield,
            s_broadcast, s_bc_native, s_bc_all, s_bc_2, s_ser_sends, s_ns_debug };

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}

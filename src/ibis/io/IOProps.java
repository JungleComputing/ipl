/* $Id$ */

package ibis.io;

import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.io package.
 */
class IOProps {
    static final String PROPERTY_PREFIX = "ibis.io.";

    static final String s_stats_nonrewritten = PROPERTY_PREFIX
            + "stats.nonrewritten";

    static final String s_stats_written = PROPERTY_PREFIX + "stats.written";

    static final String s_classloader = PROPERTY_PREFIX
            + "serialization.classloader";

    static final String s_timer = PROPERTY_PREFIX + "serialization.timer";

    static final String s_no_array_buffers = PROPERTY_PREFIX + "noarraybuffers";

    static final String s_timer_in = PROPERTY_PREFIX
            + "serialization.timer.input";

    static final String s_timer_out = PROPERTY_PREFIX
            + "serialization.timer.output";

    static final String s_conversion = PROPERTY_PREFIX + "conversion";

    static final String s_cache = PROPERTY_PREFIX + "allocator";

    static final String s_cache_max = PROPERTY_PREFIX + "allocator.size";

    static final String s_cache_stats = PROPERTY_PREFIX
            + "allocator.statistics";

    static final String s_dbg = PROPERTY_PREFIX + "debug";

    static final String s_asserts = PROPERTY_PREFIX + "assert";

    static final String s_small_array_bound = PROPERTY_PREFIX
            + "smallarraybound";

    static final String s_hash_asserts = PROPERTY_PREFIX + "hash.assert";

    static final String s_hash_stats = PROPERTY_PREFIX + "hash.stats";

    static final String s_hash_timings = PROPERTY_PREFIX + "hash.timings";

    private static final String[] sysprops = { s_stats_nonrewritten,
            s_stats_written, s_classloader, s_timer, s_timer_in, s_timer_out,
            s_conversion, s_cache, s_cache_max, s_cache_stats, s_dbg,
            s_asserts, s_small_array_bound, s_hash_asserts, s_hash_stats,
            s_hash_timings };

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }
}

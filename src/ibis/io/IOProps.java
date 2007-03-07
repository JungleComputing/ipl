/* $Id$ */

package ibis.io;

import ibis.ipl.IbisFactory;
import ibis.ipl.IbisProperties;
import ibis.util.TypedProperties;

/**
 * Collects all system properties used by the ibis.io package.
 */
class IOProps {
    static final TypedProperties properties = new TypedProperties(
            IbisProperties.getConfigProperties(IbisProperties
                    .getHardcodedProperties()));

    static final String PROPERTY_PREFIX = "ibis.io.";

    static final String s_stats_nonrewritten = PROPERTY_PREFIX
            + "stats.nonrewritten";

    static final String s_stats_written = PROPERTY_PREFIX + "stats.written";

    static final String s_classloader = PROPERTY_PREFIX
            + "serialization.classloader";

    static final String s_timer = PROPERTY_PREFIX + "serialization.timer";

    static final String s_no_array_buffers = PROPERTY_PREFIX + "noarraybuffers";

    static final String s_conversion = PROPERTY_PREFIX + "conversion";

    static final String s_cache = PROPERTY_PREFIX + "allocator";

    static final String s_cache_max = s_cache + ".size";

    static final String s_cache_stats = s_cache + ".statistics";

    static final String s_buffer_size = PROPERTY_PREFIX + "buffer.size";

    static final String s_array_buffer = PROPERTY_PREFIX + "array.buffer";

    static final String s_dbg = PROPERTY_PREFIX + "debug";

    static final String s_asserts = PROPERTY_PREFIX + "assert";

    static final String s_small_array_bound = PROPERTY_PREFIX
            + "smallarraybound";

    static final String s_hash_asserts = PROPERTY_PREFIX + "hash.assert";

    static final String s_hash_stats = PROPERTY_PREFIX + "hash.stats";

    static final String s_hash_timings = PROPERTY_PREFIX + "hash.timings";

    static final String s_hash_resize = PROPERTY_PREFIX + "hash.resize";

    private static final String[] sysprops = { s_stats_nonrewritten,
            s_stats_written, s_classloader, s_timer, s_conversion, s_cache,
            s_cache_max, s_cache_stats, s_dbg, s_asserts, s_small_array_bound,
            s_hash_asserts, s_hash_stats, s_hash_timings, s_hash_resize,
            s_buffer_size, s_array_buffer };

    static {
        properties.checkProperties(PROPERTY_PREFIX, sysprops, null, true);
    }
}

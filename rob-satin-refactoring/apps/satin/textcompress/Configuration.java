// File: $Id$
//
// A set of configuration constants.

class Configuration {
    // The number of backreferences that is maintained for each hash slot.
    static final int LOOKAHEAD_DEPTH = 6;
    static final int ALPHABET_SIZE = 128;
    static final int MINIMAL_SPAN = 4;
    static final int MAX_SHORTENING = 20;
};

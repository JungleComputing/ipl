// File: $Id$
//
// A set of configuration constants.

interface Configuration {
    // The number of backreferences that is maintained for each hash slot.
    static final int LOOKAHEAD_DEPTH = 6;
    static final int MINIMAL_SPAN = 4;
    static final int MAX_SHORTENING = 20;
    static final int DEFAULT_TOP = 2;
    static final int DEFAULT_LOOKAHEAD = 4;

    static final boolean traceAdding = false;
    static final boolean traceMatches = false;
    static final boolean traceLookahead = false;
    static final boolean traceCompressionCosts = true;
    static final boolean traceIntermediateGrammars = false;

    static final boolean doVerification = false;
};

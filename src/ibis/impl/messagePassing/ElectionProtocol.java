package ibis.impl.messagePassing;

import ibis.util.TypedProperties;

/**
 * messagePassing Ibis implementation of Ibis election: shared constants
 */
interface ElectionProtocol {
    static final byte ELECTION = 99;

    static final boolean NEED_ELECTION = TypedProperties.booleanProperty(MPProps.s_election, true);
}

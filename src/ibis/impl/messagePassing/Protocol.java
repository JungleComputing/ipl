package ibis.impl.messagePassing;

interface Protocol {
    static final byte NEW_RECEIVER = 1;
    static final byte NEW_MESSAGE = 2;
    static final byte FREE = 3;
}

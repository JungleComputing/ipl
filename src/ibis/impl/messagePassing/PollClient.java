package ibis.impl.messagePassing;

/**
 * Interface to allow classes to poll during a wait.
 */
interface PollClient {

    PollClient next();

    PollClient prev();

    void setNext(PollClient c);

    void setPrev(PollClient c);

    boolean satisfied();

    void wakeup();

    void waitNonPolling(long timeout);

    Thread thread();

    void setThread(Thread thread);

}

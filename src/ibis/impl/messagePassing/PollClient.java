package ibis.impl.messagePassing;

interface PollClient {

    PollClient next();
    PollClient prev();
    void setNext(PollClient c);
    void setPrev(PollClient c);

    boolean satisfied();

    void wakeup();

    void poll_wait(long timeout);

    Thread thread();
    void setThread(Thread thread);

}

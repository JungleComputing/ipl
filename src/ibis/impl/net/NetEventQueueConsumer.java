package ibis.impl.net;

/**
 * Provide an upcall interface to react upon incoming {@linkplain
 * ibis.impl.net.NetEvent events} from an {@linkplain
 * ibis.impl.net.NetEventQueue event queue}.
 */
public interface NetEventQueueConsumer {

    public void event(NetEvent event);
}


package ibis.impl.net;

/**
 * Provide an upcall interface to react upon incoming {@linkplain NetEvent events} from an {@linkplain NetEventQueue event queue}.
 */
public interface NetEventQueueConsumer {

        public void event(NetEvent event);
}


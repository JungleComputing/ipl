package ibis.ipl.impl.net;

/**
 * Provide an upcall interface to react upon incoming {@linkplain NetEvent events} from an {@linkplain NetEventQueue event queue} by the means of an {@linkplain NetEventQueueListener event listener}.
 */
public interface NetEventQueueConsumer {

        public void event(NetEvent event);
}


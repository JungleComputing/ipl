package ibis.ipl.registry.btcentral.server;

import ibis.ipl.registry.btcentral.Member;
import ibis.util.ThreadPool;

import java.util.LinkedHashSet;
import java.util.Set;

public class OndemandEventPusher implements Runnable {

    private final Pool pool;

    private final Set<Member> q;

    public OndemandEventPusher(Pool pool) {
        this.pool = pool;
        q = new LinkedHashSet<Member>();

        ThreadPool.createNew(this, "Pusher");
    }

    public synchronized void enqueue(Member member) {
        q.add(member);
    }
    
    public synchronized Member dequeue() {
        while (q.isEmpty()) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                // /IGNORE
            }
        }
        Member result = q.iterator().next();
        q.remove(result);

        return result;
    }

    public void run() {

        while (true) {
            Member next = dequeue();

            if (next == null) {
                // q empty, pool ended
                return;
            }

            pool.push(next, true, false);
        }
    }
}

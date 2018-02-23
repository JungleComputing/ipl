/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.registry.central.server;

import ibis.ipl.registry.central.Member;
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

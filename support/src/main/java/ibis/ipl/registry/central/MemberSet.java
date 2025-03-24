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
package ibis.ipl.registry.central;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import ibis.ipl.impl.IbisIdentifier;

public interface MemberSet {

    public int size();

    public void add(Member member);

    public Member remove(IbisIdentifier identifier);

    public boolean contains(IbisIdentifier identifier);

    public boolean contains(Member member);

    public Member get(IbisIdentifier identifier);

    // return a member from what identifier.name() returns (!= identifier.getID())
    public Member get(String name);

    public int getMinimumTime();

    public Member getLeastRecentlySeen();

    public Member get(int index);

    public Member getRandom();

    public Member[] getRandom(int size);

    public Member[] asArray();

    public void init(DataInputStream in) throws IOException;

    public void writeTo(DataOutputStream out) throws IOException;

    public List<Event> getJoinEvents();

    public Member[] getChildren(IbisIdentifier ibis);

    public Member[] getRootChildren();
}

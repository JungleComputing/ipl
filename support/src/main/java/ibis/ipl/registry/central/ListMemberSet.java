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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import ibis.ipl.impl.IbisIdentifier;

public final class ListMemberSet implements MemberSet {

    private final ArrayList<Member> list;

    private final Random random;

    public ListMemberSet() {
        list = new ArrayList<>();
        random = new Random();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void add(Member member) {
        list.add(member);
    }

    @Override
    public Member remove(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                Member result = list.remove(i);
                return result;
            }
        }
        return null;
    }

    @Override
    public boolean contains(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (Member element : list) {
            if (element.getID().equals(ID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Member member) {
        return contains(member.getIbis());
    }

    @Override
    public Member get(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (Member element : list) {
            if (element.getID().equals(ID)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public Member get(String name) {
        for (Member element : list) {
            if (element.getIbis().name().equals(name)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public int getMinimumTime() {
        if (list.isEmpty()) {
            return -1;
        }

        int minimum = list.get(0).getCurrentTime();

        for (Member element : list) {
            if (element.getCurrentTime() < minimum) {
                minimum = element.getCurrentTime();
            }
        }
        return minimum;
    }

    @Override
    public Member getLeastRecentlySeen() {
        if (list.isEmpty()) {
            return null;
        }

        Member oldest = list.get(0);

        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).getTime() < oldest.getTime()) {
                oldest = list.get(i);
            }
        }

        return oldest;
    }

    @Override
    public Member get(int index) {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    @Override
    public Member getRandom() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(size()));
    }

    @Override
    public Member[] getRandom(int size) {
        ArrayList<Member> result = new ArrayList<>();
        BitSet added = new BitSet();

        if (size > list.size()) {
            size = list.size();
        }

        while (result.size() < size) {
            int next = random.nextInt(list.size());
            if (!added.get(next)) {
                // not added yet

                result.add(list.get(next));

                // remember we already added this member.
                added.set(next);
            }
        }

        return result.toArray(new Member[0]);
    }

    @Override
    public Member[] asArray() {
        return list.toArray(new Member[0]);
    }

    @Override
    public void init(DataInputStream in) throws IOException {
        int nrOfMembers = in.readInt();

        if (nrOfMembers < 0) {
            throw new IOException("negative list size recieved" + nrOfMembers);
        }

        for (int i = 0; i < nrOfMembers; i++) {
            list.add(new Member(in));
        }
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(list.size());
        for (Member member : list) {
            member.writeTo(out);
        }
    }

    @Override
    public List<Event> getJoinEvents() {
        ArrayList<Event> result = new ArrayList<>();

        for (Member member : list) {
            result.add(member.getEvent());
        }

        return result;

    }

    /**
     * List does not have parents/children
     */
    @Override
    public Member[] getChildren(IbisIdentifier ibis) {
        return new Member[0];
    }

    @Override
    public Member[] getRootChildren() {
        return asArray();
    }

}

package ibis.ipl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public final class ListMemberSet implements MemberSet {

    private final ArrayList<Member> list;

    private final Random random;

    public ListMemberSet() {
        list = new ArrayList<Member>();
        random = new Random();
    }

    public int size() {
        return list.size();
    }

    public void add(Member member) {
        list.add(member);
    }

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

    public boolean contains(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Member member) {
        return contains(member.getIbis());
    }

    public Member get(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return list.get(i);
            }
        }
        return null;
    }
    
    public Member get(String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getIbis().name().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    

    public int getMinimumTime() {
        if (list.isEmpty()) {
            return -1;
        }

        int minimum = list.get(0).getCurrentTime();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getCurrentTime() < minimum) {
                minimum = list.get(i).getCurrentTime();
            }
        }
        return minimum;
    }

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

    public Member get(int index) {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public Member getRandom() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(size()));
    }

    public Member[] getRandom(int size) {
        ArrayList<Member> result = new ArrayList<Member>();
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

    public Member[] asArray() {
        return list.toArray(new Member[0]);
    }

    public void init(DataInputStream in) throws IOException {
        int nrOfMembers = in.readInt();
        
        if (nrOfMembers < 0) {
            throw new IOException("negative list size recieved" + nrOfMembers);
        }
        
        for (int i = 0; i < nrOfMembers; i++) {
            list.add(new Member(in));
        }
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(list.size());
        for (Member member: list) {
            member.writeTo(out);
        }
    }

    public List<Event> getJoinEvents() {
        ArrayList<Event> result = new ArrayList<Event>();
        
        for(Member member: list) {
            result.add(member.getEvent());
        }
        
        return result;
        
    }

    /**
     * List does not have parents/children
     */
    public Member[] getChildren(IbisIdentifier ibis) {
        return new Member[0];
    }

    public Member[] getRootChildren() {
        return asArray();
    }
    
    

}

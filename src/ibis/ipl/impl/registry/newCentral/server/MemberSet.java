package ibis.ipl.impl.registry.newCentral.server;


import ibis.ipl.impl.IbisIdentifier;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

final class MemberSet {

    private final ArrayList<Member> list;

    private final Random random;

    MemberSet() {
        list = new ArrayList<Member>();
        random = new Random();
    }

    int size() {
        return list.size();
    }

    void add(Member member) {
        list.add(member);
    }

    Member remove(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                Member result = list.remove(i);
                return result;
            }
        }
        return null;
    }

    boolean contains(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return true;
            }
        }
        return false;
    }
    
    boolean contains(Member member) {
        return contains(member.getIbis());
    }
    
    Member get(IbisIdentifier identifier) {
        String ID = identifier.getID();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return list.get(i);
            }
        }
        return null;
    }
    
    Member getLeastRecentlySeen() {
        if (list.isEmpty()) {
            return null;
        }
        
        Member oldest = list.get(0);
        
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).getLastSeen() < oldest.getLastSeen()) {
                oldest = list.get(i);
            }
        }
        
        return oldest;
    }
            

    Member get(int index) {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    Member getRandom() {
        return list.get(random.nextInt(size()));
    }

    Member[] getRandom(int size) {
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

                // remember we already added this memeber.
                added.set(next);
            }
        }

        return result.toArray(new Member[0]);
    }
    
    Member[] asArray() {
        return list.toArray(new Member[0]);
    }

}



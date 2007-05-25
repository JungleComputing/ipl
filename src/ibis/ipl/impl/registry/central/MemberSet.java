package ibis.ipl.impl.registry.central;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
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

    boolean remove(String ID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    boolean contains(String ID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return true;
            }
        }
        return false;
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
    
    @SuppressWarnings("unchecked")
    List<Member> asList() {
        return (List<Member>) list.clone();
    }
}

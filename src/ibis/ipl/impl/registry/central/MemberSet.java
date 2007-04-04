package ibis.ipl.impl.registry.central;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

public class MemberSet {

    private final ArrayList<Member> list;

    private final Random random;

    int behindNext;

    MemberSet() {
        list = new ArrayList<Member>();
        random = new Random();
        behindNext = 0;
    }

    public int size() {
        return list.size();
    }

    public void add(Member member) {
        list.add(member);
    }

    public boolean remove(String ID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(String ID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return true;
            }
        }
        return false;
    }

    public Member get(int index) {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public Member getRandom() {
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

                // remember we already added this memeber.
                added.set(next);
            }
        }

        return result.toArray(new Member[0]);
    }
}

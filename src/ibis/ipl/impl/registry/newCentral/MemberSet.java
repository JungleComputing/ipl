package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.impl.IbisIdentifier;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

final class MemberSet {

    private final ArrayList<IbisIdentifier> list;

    private final Random random;

    MemberSet() {
        list = new ArrayList<IbisIdentifier>();
        random = new Random();
    }

    int size() {
        return list.size();
    }

    void add(IbisIdentifier ibis) {
        list.add(ibis);
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
    
    boolean contains(IbisIdentifier ibis) {
    	return contains(ibis.getID());
    }

    boolean contains(String ID) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getID().equals(ID)) {
                return true;
            }
        }
        return false;
    }

    IbisIdentifier get(int index) {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    IbisIdentifier getRandom() {
        return list.get(random.nextInt(size()));
    }

    IbisIdentifier[] getRandom(int size) {
        ArrayList<IbisIdentifier> result = new ArrayList<IbisIdentifier>();
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

        return result.toArray(new IbisIdentifier[0]);
    }
    
    @SuppressWarnings("unchecked")
    List<IbisIdentifier> asList() {
        return (List<IbisIdentifier>) list.clone();
    }
}

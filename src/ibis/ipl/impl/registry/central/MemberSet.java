package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public interface MemberSet {

    public int size();

    public void add(Member member);

    public Member remove(IbisIdentifier identifier);

    public boolean contains(IbisIdentifier identifier);

    public boolean contains(Member member);

    public Member get(IbisIdentifier identifier);

    public int getMinimumTime();

    public Member getLeastRecentlySeen();

    public Member get(int index);

    public Member getRandom();

    public Member[] getRandom(int size);

    public Member[] asArray();

    public void init(DataInput in) throws IOException;

    public void writeTo(DataOutput out) throws IOException;

    public List<Event> getJoinEvents();
}

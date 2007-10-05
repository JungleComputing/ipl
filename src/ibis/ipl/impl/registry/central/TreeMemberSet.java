package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public final class TreeMemberSet implements MemberSet {

    public TreeMemberSet() {
    }

    public void add(Member member) {
        // TODO Auto-generated method stub
        
    }

    public Member[] asArray() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean contains(IbisIdentifier identifier) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean contains(Member member) {
        // TODO Auto-generated method stub
        return false;
    }

    public Member get(IbisIdentifier identifier) {
        // TODO Auto-generated method stub
        return null;
    }

    public Member get(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Event> getJoinEvents() {
        // TODO Auto-generated method stub
        return null;
    }

    public Member getLeastRecentlySeen() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getMinimumTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Member getRandom() {
        // TODO Auto-generated method stub
        return null;
    }

    public Member[] getRandom(int size) {
        // TODO Auto-generated method stub
        return null;
    }

    public void init(DataInput in) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public Member remove(IbisIdentifier identifier) {
        // TODO Auto-generated method stub
        return null;
    }

    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void writeTo(DataOutput out) throws IOException {
        // TODO Auto-generated method stub
        
    }



}

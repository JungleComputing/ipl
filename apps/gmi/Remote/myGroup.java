import ibis.gmi.GroupInterface;

interface myGroup extends GroupInterface { 

    public void put(Object o);
    public Object get();
    public Object put_get(Object o);
} 

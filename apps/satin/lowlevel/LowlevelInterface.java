/* $Id$ */

interface LowlevelInterface extends ibis.satin.Spawnable {
    public void spawn_void();

    public int spawn_int(int i);

    public SerializableObject spawn_Object(SerializableObject o);

    public void inlet_spawn_void() throws DoneVoid;

    public void inlet_spawn_int(int i) throws DoneInt;

    public void inlet_spawn_Object(SerializableObject o) throws DoneObject;

    public void inlet_spawn_void_no_new() throws DoneVoid;

    public void inlet_spawn_int_no_new(int i) throws DoneInt;

    public void inlet_spawn_Object_no_new(SerializableObject o)
            throws DoneObject;

    public void inlet_spawn_void_overridden() throws DoneVoidOverridden;

    public void inlet_spawn_int_overridden(int i) throws DoneIntOverridden;

    public void inlet_spawn_Object_overridden(SerializableObject o)
            throws DoneObjectOverridden;
}
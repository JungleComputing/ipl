interface LowlevelInterface extends ibis.satin.Spawnable {
	public void spawn_void();
	public int spawn_int(int i);
	public Object spawn_Object(Object o);

	public void inlet_spawn_void() throws DoneVoid;
	public void inlet_spawn_int(int i) throws DoneInt;
	public void inlet_spawn_Object(Object o) throws DoneObject;

	public void inlet_spawn_void_no_new() throws DoneVoid;
	public void inlet_spawn_int_no_new(int i) throws DoneInt;
	public void inlet_spawn_Object_no_new(Object o) throws DoneObject;

	public void inlet_spawn_void_overridden() throws DoneVoidOverridden;
	public void inlet_spawn_int_overridden(int i) throws DoneIntOverridden;
	public void inlet_spawn_Object_overridden(Object o) throws DoneObjectOverridden;
}

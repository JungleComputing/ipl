interface i_Slave extends ibis.group.GroupInterface { 

	public void setValues(int i, double[] values);
	public double[] getValues(int i);
	public void barrier();
} 

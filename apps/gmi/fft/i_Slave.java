interface i_Slave extends ibis.gmi.GroupInterface {

    public void setValues(int i, double[] values);

    public double[] getValues(int i);

    public void barrier();
}
import ibis.group.GroupInterface;

interface i_Asp extends GroupInterface {
    public void transfer(int[] row, int k);
    public void done();
}

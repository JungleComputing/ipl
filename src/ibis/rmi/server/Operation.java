package ibis.rmi.server;

public class Operation
{
    private String operation;
    
    public Operation(String op) {
	operation = op;
    }
    
    public String getOperation() {
	return operation;
    }
   
    public String toString() {
	return operation;
    }
}

class Abort extends Exception implements java.io.Serializable {
    byte[] result;

    Abort(byte[] result) {
	this.result = result;
    }
}

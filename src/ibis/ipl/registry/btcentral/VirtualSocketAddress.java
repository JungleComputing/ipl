package ibis.ipl.registry.btcentral;

public class VirtualSocketAddress {

		String address;
		
		public VirtualSocketAddress(String addr){
			address = addr;		
		}
	
		public String toString(){
			return address;
		}
		
		public byte[] toBytes(){
			return address.getBytes();
		}
		
		public boolean equals(Object other){
			if(other.getClass() == this.getClass())
				return address.equals(((VirtualSocketAddress)other).toString());
			else 
				return false; 
		}
		
		static public VirtualSocketAddress fromBytes(byte[] source, int offset){			
			//TODO: not sure...
			return new VirtualSocketAddress(new String(source));
		}
}

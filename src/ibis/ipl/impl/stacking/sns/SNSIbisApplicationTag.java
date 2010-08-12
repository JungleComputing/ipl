package ibis.ipl.impl.stacking.sns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

public class SNSIbisApplicationTag implements Serializable{

	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	byte[] snsTag;
	byte[] appTag;

    /**
     * Creates an Ibis tag that hold both SNS application tag and the original application tag
     * 
     * @return the extended application tag object.
     */
	public SNSIbisApplicationTag(byte[] SNSIbisTag, byte[] appTag){
		this.snsTag = SNSIbisTag;
		this.appTag = appTag;
	}
	
    /**
     * Creates an SNS application tag from tag data of underlying IPL implementation
     * 
     * @return the extended application tag object.
     */
	public SNSIbisApplicationTag(byte[] data){
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bis);
			
			int snsTagLength = dis.readInt(); 
			snsTag = new byte[snsTagLength]; 
			dis.read(snsTag, 0, snsTagLength); 
			
			if (dis.available() > 0) {
				int appTagLength = dis.readInt();
				appTag= new byte[appTagLength]; 
				dis.read(appTag, 0, appTagLength);
			}		
		} catch (IOException e) {
            throw new RuntimeException("could not read the Ibis application tag", e);
		} 
	}
	
	public byte[] getBytes() throws IOException{
	      ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      DataOutputStream dos = new DataOutputStream(bos);
	      dos.writeInt(snsTag.length);
	      dos.write(snsTag);
	      
	      if (appTag != null) {
		      dos.writeInt(appTag.length);
		      dos.write(appTag);
	      }
	      
	      dos.flush();
	      dos.close();
	      return bos.toByteArray();
	}
	
	public String SNSTagAsString() {
        if (snsTag == null) {
            return null;
        }
        
        try {
            return new String(snsTag, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("could not convert tag to string", e);
        }	
	}
}
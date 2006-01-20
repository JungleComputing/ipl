package ibis.mpj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * Buffer and unbuffer operations
 *  
 */
public class BufferOps {
	
	protected synchronized static int unBuffer(Object inbuf, Object outbuf, int offset, int count) {
		if (outbuf instanceof byte[]) {
			return(unBufferByte((byte[])inbuf, 0, (byte[])outbuf, offset, count));
		}
		else if (outbuf instanceof char[]) {
			return(unBufferChar((byte[])inbuf, 0, (char[])outbuf, offset, count));
		}
		else if (outbuf instanceof short[]) {
			return(unBufferShort((byte[])inbuf, 0, (short[])outbuf, offset, count));
		}
		else if (outbuf instanceof boolean[]) {
			return(unBufferBoolean((byte[])inbuf, 0, (boolean[])outbuf, offset, count));
		}
		else if (outbuf instanceof int[]) {
			return(unBufferInt((byte[])inbuf, 0, (int[])outbuf, offset, count));
		}
		else if (outbuf instanceof long[]) {
			return(unBufferLong((byte[])inbuf, 0, (long[])outbuf, offset, count));
		}
		else if (outbuf instanceof float[]) {
			return(unBufferFloat((byte[])inbuf, 0, (float[])outbuf, offset, count));
		}
		else if (outbuf instanceof double[]) {
			return(unBufferDouble((byte[])inbuf, 0, (double[])outbuf, offset, count));
		}
		else {
			return(unBufferObject((byte[])inbuf, 0, (Object[])outbuf, offset, count));
			
		}

	
	}
	
	protected synchronized static int bufferByte(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((byte[])srcBuf).length) {
			count = ((byte[])srcBuf).length - offset;
		}
		
		if ((position + count) > destBuf.length ) {
			int newCount = destBuf.length - position;
			if (newCount < count) {
				count = newCount;
			}
		}
		System.arraycopy(srcBuf, offset, destBuf, position, count);
		
		return(count+position);
		
		
	}
	
	protected synchronized static int bufferChar(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((char[])srcBuf).length) {
			count = ((char[])srcBuf).length - offset;
		}

		if ((position + (count * MPJ.CHAR.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.CHAR.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		
		for (int i = 0; i < count; i++) {
			destBuf[position++] = (byte)((((char[])srcBuf)[offset+i] & 0xFF00) >>> 8);
			destBuf[position++] = (byte)((((char[])srcBuf)[offset+i] & 0x00FF));
		}
		return(position);
		
		
	}

	protected synchronized static int bufferShort(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((short[])srcBuf).length) {
			count = ((short[])srcBuf).length - offset;
		}

		if ((position + (count * MPJ.SHORT.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.SHORT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		
		for (int i = offset; i < count+offset; i++) {
			destBuf[position++] = (byte)((((short[])srcBuf)[i] & 0xFF00) >>> 8);
			destBuf[position++] = (byte)((((short[])srcBuf)[i] & 0x00FF));
		}
		return(position);
		
		
	}
	
	protected synchronized static int bufferBoolean(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((boolean[])srcBuf).length) {
			count = ((boolean[])srcBuf).length - offset;
		}
		
		if ((position + (count * MPJ.BOOLEAN.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.BOOLEAN.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		
		for (int i = offset; i < count+offset; i++) {
			if (((boolean[])srcBuf)[i]) {
				destBuf[position] = 1;
			}
			else {
				destBuf[position] = 0;
			}
			position++;
		}
		return(position);
		
		
	}
	
	protected synchronized static int bufferInt(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((int[])srcBuf).length) {
			count = ((int[])srcBuf).length - offset;
		}
		if ((position + (count * MPJ.INT.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.INT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			destBuf[position] = (byte)((((int[])srcBuf)[i] & 0xFF000000) >>> 24);
			position++;
			destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x00FF0000) >>> 16);
			position++;
			destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x0000FF00) >>> 8);
			position++;
			destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x000000FF));
			position++;
		}
		return(position);
		
		
	}
	
	protected synchronized static int bufferLong(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((long[])srcBuf).length) {
			count = ((long[])srcBuf).length - offset;
		}
		if ((position + (count * MPJ.LONG.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.LONG.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0xFF00000000000000L) >>> 56);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00FF000000000000L) >>> 48);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x0000FF0000000000L) >>> 40);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x000000FF00000000L) >>> 32);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00000000FF000000L) >>> 24);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x0000000000FF0000L) >>> 16);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x000000000000FF00L) >>> 8);
			position++;
			destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00000000000000FFL));
			position++;
		}
		return(position);
		
		
	}
	
	protected synchronized static int bufferFloat(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((float[])srcBuf).length) {
			count = ((float[])srcBuf).length - offset;
		}
		
		if ((position + (count * MPJ.FLOAT.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.FLOAT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			int intBits = Float.floatToIntBits(((float[])srcBuf)[i]);
			destBuf[position] = (byte)((intBits) >>> 24);
			position++;
			destBuf[position] = (byte)((intBits) >>> 16);
			position++;
			destBuf[position] = (byte)((intBits) >>> 8);
			position++;
			destBuf[position] = (byte)((intBits));
			position++;
		}
		return(position);
		
	}
	
	protected synchronized static int bufferDouble(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((double[])srcBuf).length) {
			count = ((double[])srcBuf).length - offset;
		}
		
		if ((position + (count * MPJ.DOUBLE.getByteSize())) > (destBuf.length)) {
			
			int newCount = (destBuf.length - position) / MPJ.DOUBLE.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			long longBits = Double.doubleToLongBits(((double[])srcBuf)[i]);
			destBuf[position] = (byte)((longBits) >>> 56);
			position++;
			destBuf[position] = (byte)((longBits) >>> 48);
			position++;
			destBuf[position] = (byte)((longBits) >>> 40);
			position++;
			destBuf[position] = (byte)((longBits) >>> 32);
			position++;
			destBuf[position] = (byte)((longBits) >>> 24);
			position++;
			destBuf[position] = (byte)((longBits) >>> 16);
			position++;
			destBuf[position] = (byte)((longBits) >>> 8);
			position++;
			destBuf[position] = (byte)((longBits));
			position++;
		}
		return(position);
		
		
	}
	
	protected synchronized static int bufferObject(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((Object[])srcBuf).length) {
			count = ((Object[])srcBuf).length - offset;
		}
		try {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);

			for (int i = offset; i < count+offset; i++) {
				objectOutStream.writeObject(((Object[])srcBuf)[i]);
				
			}

			count = byteOutStream.size();
			
			if ((count+position) <= destBuf.length) {
				System.arraycopy(byteOutStream.toByteArray(), 0, destBuf, position, count);
			}
			
			return(count + position);	
		} 
		catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return(0);
		}
		
		
	}
	
	
	protected synchronized static int unBufferByte(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((byte[])destBuf).length) {
			count = ((byte[])destBuf).length - offset;
		}
		
		if ((count + position) > srcBuf.length) {
			
			int newCount = srcBuf.length - position;
			if ( newCount < count) {
			    count = newCount;
			}
		}
		System.arraycopy(srcBuf, position, destBuf, offset, count);
		
		return(count + position);
	}
	
	protected synchronized static int unBufferChar(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((char[])destBuf).length) {
			count = ((char[])destBuf).length - offset;
		}

		if ((position + (count * MPJ.CHAR.getByteSize())) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position)/ MPJ.CHAR.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			
			((char[])destBuf)[i] = (char)(((srcBuf[position] & 0xFF00) << 8)
										+(srcBuf[position+1] & 0x00FF));
			position += MPJ.CHAR.getByteSize();
		}
		return(position);
		
		
	}



	protected synchronized static int unBufferShort(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((short[])destBuf).length) {
			count = ((short[])destBuf).length - offset;
		}

		if (((count * MPJ.SHORT.getByteSize()) + position) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length -position) / MPJ.SHORT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			
			((short[])destBuf)[i] = (short)(((srcBuf[position] & 0xFF00) << 8)
									      +(srcBuf[position+1] & 0x00FF));
			position += MPJ.SHORT.getByteSize();
		}
		return(position);
		
		
	}

	
	
	
	protected synchronized static int unBufferBoolean(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((boolean[])destBuf).length) {
			count = ((boolean[])destBuf).length - offset;
		}

		if (((count * MPJ.BOOLEAN.getByteSize()) + position) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position) / MPJ.CHAR.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			
			if (srcBuf[position] != 0) {
				((boolean[])destBuf)[i] = true;
			}
			else {
				((boolean[])destBuf)[i] = false;
			}
			position++;
		}
		return(position);
		
		
	}

	
	
	protected synchronized static int unBufferInt(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((int[])destBuf).length) {
			count = ((int[])destBuf).length - offset;
		}

		if (((count * MPJ.INT.getByteSize()) + position) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position) / MPJ.INT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			
		
			((int[])destBuf)[i] = (((srcBuf[position]& 0xff) << 24)
		            				  + ((srcBuf[position+1]& 0xff) << 16)
									  + ((srcBuf[position+2]& 0xff) << 8)
									  + ((srcBuf[position+3]& 0xff)));
			
			position += MPJ.INT.getByteSize();
		}
		return(position);
		
		
	}

	protected synchronized static int unBufferLong(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((long[])destBuf).length) {
			count = ((long[])destBuf).length - offset;
		}

		if (((count * MPJ.LONG.getByteSize()) + position) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position) / MPJ.LONG.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {

			((long[])destBuf)[i] = (((srcBuf[position]  & 0xff) << 56)
						   	   	       + ((srcBuf[position+1] & 0xff) << 48)
									   + ((srcBuf[position+2] & 0xff) << 40)
									   + ((srcBuf[position+3] & 0xff) << 32)
			                           + ((srcBuf[position+4] & 0xff) << 24)
			                           + ((srcBuf[position+5] & 0xff) << 16)
			                           + ((srcBuf[position+6] & 0xff) << 8)
									   + ((srcBuf[position+7] & 0xff)));
			
			position += MPJ.LONG.getByteSize();
		}
		return(position);
	}

	
	
	
	protected synchronized static int unBufferFloat(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((float[])destBuf).length) {
			count = ((float[])destBuf).length - offset;
		}

		if (((count * MPJ.FLOAT.getByteSize()) + position) > (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position) / MPJ.FLOAT.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {
			int intBits = (((srcBuf[position] & 0xff) << 24)
		            		+ ((srcBuf[position+1] & 0xff) << 16)
							+ ((srcBuf[position+2] & 0xff) << 8)
							+ ((srcBuf[position+3] & 0xff)));

			((float[])destBuf)[i] = Float.intBitsToFloat(intBits);
			position += MPJ.FLOAT.getByteSize();
		}
		return(position);
		
		
	}

	
	protected synchronized static int unBufferDouble(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
			
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((double[])destBuf).length) {
			count = ((double[])destBuf).length - offset;
		}

		if (((count * MPJ.DOUBLE.getByteSize()) + position)> (srcBuf.length)) {
			
			int newCount = (srcBuf.length - position) / MPJ.DOUBLE.getByteSize();
			if ( newCount < count) {
			    count = newCount;
			}
		}

		for (int i = offset; i < count+offset; i++) {

			
			long longBits = ((((long)srcBuf[position] & 0xff) << 56)
					    	   + (((long)srcBuf[position+1] & 0xff) << 48)
							   + (((long)srcBuf[position+2] & 0xff) << 40)
							   + (((long)srcBuf[position+3] & 0xff) << 32)
			                   + (((long)srcBuf[position+4] & 0xff) << 24)
			                   + (((long)srcBuf[position+5] & 0xff) << 16)
			                   + (((long)srcBuf[position+6] & 0xff) << 8)
							   + (((long)srcBuf[position+7] & 0xff)));
			

			((double[])destBuf)[i] = Double.longBitsToDouble(longBits);
			position += MPJ.DOUBLE.getByteSize();
		}
		return(position);
		
		
	}

	
	
	protected synchronized static int unBufferObject(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		if ((offset + count) > ((Object[])destBuf).length) {
			count = ((Object[])destBuf).length - offset;
		}

		
		try {
			
			byte[] tmp = new byte[srcBuf.length - position];
			System.arraycopy(srcBuf, position, tmp, 0, tmp.length);
			
			ByteArrayInputStream byteInputStream = new ByteArrayInputStream(tmp);
			ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
			
		
			for (int i=offset; i < count+offset; i++) {
				((Object[])destBuf)[i] = objectInputStream.readObject();
				
			}
			position = srcBuf.length - byteInputStream.available();
			
			return(position);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return(0);
		}
	}


	
	
}

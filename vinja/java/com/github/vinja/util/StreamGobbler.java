/** 
 
 * @author wangsn
 * @since 1.0
 * @version $Id: StreamGobbler.java, v 1.0 2010-12-6 ����07:09:13 wangsn Exp $
 */
package com.github.vinja.util;

import java.io.InputStream;

public class StreamGobbler extends Thread {
	
	private StringBuffer buffer;
	
	private InputStream is  = null;
	private byte[] byteBuffer = new byte[4098];
	
	public StreamGobbler(StringBuffer buffer, InputStream is) {
		
		this.is = is;
		this.buffer = buffer;
	}
	
	 public void run() {
            try {
                while (true) {
                	int count = is.read(byteBuffer);
                	if (count == -1) break;
                	synchronized(buffer) {
	                	buffer.append(new String(byteBuffer,0,count));
                	}
                }
            } catch (Exception e) {
	        	throw new Error(e);
        	}
        }
}
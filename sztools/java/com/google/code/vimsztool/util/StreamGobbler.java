/** 
 
 * @author wangsn
 * @since 1.0
 * @version $Id: StreamGobbler.java, v 1.0 2010-12-6 ����07:09:13 wangsn Exp $
 */
package com.google.code.vimsztool.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
	
	private BufferedReader br;
	private StringBuffer buffer;
	
	public StreamGobbler(StringBuffer buffer, InputStream is) {
		this.br = new BufferedReader(new InputStreamReader(is));
		this.buffer = buffer;
	}
	
	 public void run() {
            String line;
            try {
                while ((line = br.readLine()) != null) {
                	synchronized(buffer) {
	                	buffer.append(line).append("\n");
                	}
                }
            } catch (Exception e) {throw new Error(e);}
        }
}
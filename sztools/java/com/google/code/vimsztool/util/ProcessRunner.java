/** 
 
 * @author wangsn
 * @since 1.0
 * @version $Id: ProcessRunner.java, v 1.0 2010-12-6 обнГ06:58:49 wangsn Exp $
 */
package com.google.code.vimsztool.util;


public class ProcessRunner {
	
	private StringBuffer buffer = new StringBuffer();
	
	public String communicate(Process process) {
		
		StreamGobbler stdOut=new StreamGobbler(buffer, process.getInputStream());
		StreamGobbler stdErr=new StreamGobbler(buffer, process.getErrorStream());
		stdOut.start();
		stdErr.start();

	    try {
	         process.waitFor();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    return buffer.toString();

	} 

}



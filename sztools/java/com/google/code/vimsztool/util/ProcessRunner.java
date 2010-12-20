/** 
 
 * @author wangsn
 * @since 1.0
 * @version $Id: ProcessRunner.java, v 1.0 2010-12-6 ����06:58:49 wangsn Exp $
 */
package com.google.code.vimsztool.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ProcessRunner {
	
	private StringBuffer buffer = new StringBuffer();
	private Process process = null;
	private Preference pref = Preference.getInstance();
	
	@SuppressWarnings("unchecked")
	public String communicate(Process process) {
		this.process = process;
		int timeOut = 20;
		String timeOutStr = pref.getValue(Preference.JDE_RUN_TIMEOUT);
		try {
			timeOut = Integer.parseInt(timeOutStr);
		} catch (NumberFormatException e) {
		}
		FutureTask<Integer> task = null;
		ExecutorService exec = Executors.newFixedThreadPool(1);
		try {
			task = new FutureTask(new Communicator(),null);
			exec.execute(task);
			task.get(timeOut, TimeUnit.SECONDS);
		} catch (TimeoutException e ) {
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			task.cancel(true);
			process.destroy();
		}
		exec.shutdown();
		return buffer.toString();
	}
	
	class Communicator implements Runnable {
		public void run() {
			StreamGobbler stdOut=new StreamGobbler(buffer, process.getInputStream());
			StreamGobbler stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdOut.start();
			stdErr.start();

		    try {
		         process.waitFor();
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}
	}

}



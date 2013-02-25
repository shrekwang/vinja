package com.google.code.vimsztool.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemJob extends Thread {

	private static Map<String,SystemJob> jobs = new HashMap<String,SystemJob>();
	private String[] cmdArray;
	private String workDir;
	private String vimServerName;
	private boolean runInShell = false;
	private String uuid;
	private String bufname;
	private String origCmdLine;
	//private Preference pref = Preference.getInstance();
	private ScheduledExecutorService exec = null;
	private StringBuffer buffer = new StringBuffer();
	private Process process ;
	private StreamGobbler stdOut = null;
	private StreamGobbler stdErr = null;
	
	public static SystemJob getJob(String uuid) {
		return jobs.get(uuid);
	}

	public SystemJob(String cmd,String vimServerName,String cmdShell, 
			String uuid,String bufname,String workDir,String origCmdLine) {
		this.cmdArray = cmd.split("::");
		this.vimServerName = vimServerName;
		this.uuid = uuid;
		this.bufname = bufname;
		this.workDir = workDir;
		this.origCmdLine = origCmdLine;
		if (cmdShell !=null && cmdShell.equalsIgnoreCase("true")) {
			runInShell = true;
		}
		jobs.put(uuid, this);
	}
	
	public synchronized String fetchResult() {
		String result =this.buffer.toString();
		this.buffer.delete(0, buffer.length());
		return result;
	}

	public void run() {
		try {
			if (runInShell) {
				String[] newCmdArray = new String[cmdArray.length+2];
				newCmdArray[0] = "cmd.exe";
				newCmdArray[1] = "/c";
				for (int i=0; i<cmdArray.length; i++) {
					newCmdArray[i+2]=cmdArray[i];
				}
				process = Runtime.getRuntime().exec(newCmdArray,null, new File(workDir));
			} else {
				process = Runtime.getRuntime().exec(cmdArray,null, new File(workDir));
			}
			stdOut=new StreamGobbler(buffer, process.getInputStream());
			stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdOut.start();
			stdErr.start();
			
			/*
			int timeOut = 60 * 10 ;
			String timeOutStr = pref.getValue(Preference.JDE_RUN_TIMEOUT);
			try {
				timeOut = Integer.parseInt(timeOutStr);
			} catch (NumberFormatException e) {
			}
			*/
	
			exec = Executors.newScheduledThreadPool(1);
	        exec.scheduleAtFixedRate(new BufferChecker(), 1, 100, TimeUnit.MILLISECONDS);
	        /*
	        exec.schedule(new Runnable() {
	            public void run() { 
					process.destroy();
	            	exec.shutdown();
            	}
	        }, timeOut, TimeUnit.SECONDS);
	        */
		
	        process.waitFor();
		    exec.shutdown();
		} catch (Exception err) {
			buffer.append(err.getMessage());
		} finally {
			if (exec !=null ) { exec.shutdown(); }
			//wait max 3 seconds till stdout gobbler finished.
			int count = 0;
			while (true) {
				if (!stdOut.isAlive())  break;
				if (count > 30 ) break;
				try {
					sleep(100);
					count++;
				}  catch (Exception e) {
				}
			}
			buffer.append("\n");
			buffer.append( "(" + origCmdLine + "  finished.)");
			//run buffer checker last time 
		    new BufferChecker().run();
		}
		
	}

	class BufferChecker implements Runnable {
		public void run() {
			synchronized (buffer) {
				if ( ! (buffer.length() > 0)) return; 
				String[] args = new String[] { uuid,bufname };
				VjdeUtil.callVimFunc(vimServerName, "FetchResult", args);
			}
		}
	}
}
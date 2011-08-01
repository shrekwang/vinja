package com.google.code.vimsztool.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemJob extends Thread {

	private static Map<String,SystemJob> jobs = new HashMap<String,SystemJob>();
	private String cmd;
	private String workDir;
	private String vimServerName;
	private boolean runInShell = false;
	private String uuid;
	private String bufname;
	private Preference pref = Preference.getInstance();
	private ScheduledExecutorService exec = null;
	private StringBuffer buffer = new StringBuffer();
	private Process process ;
	private boolean finished = false; 
	private boolean outputClosed = false;
	
	public static SystemJob getJob(String uuid) {
		return jobs.get(uuid);
	}

	public SystemJob(String cmd,String vimServerName,String cmdShell, 
			String uuid,String bufname,String workDir) {
		this.cmd = cmd;
		this.vimServerName = vimServerName;
		this.uuid = uuid;
		this.bufname = bufname;
		this.workDir = workDir;
		if (cmdShell !=null && cmdShell.equalsIgnoreCase("true")) {
			runInShell = true;
		}
		jobs.put(uuid, this);
	}
	
	public synchronized String fetchResult() {
		if (outputClosed) return "";
		String result =this.buffer.toString();
		this.buffer.delete(0, buffer.length());
		if (finished) {
			result = result + "\n" + "(" + cmd + "  finished.)";
			outputClosed = true;
		}
		return result;
	}

	public void run() {
		try {
			if (runInShell) {
				process = Runtime.getRuntime().exec(new String[] {"cmd.exe", "/c",cmd},null, new File(workDir));
			} else {
				process = Runtime.getRuntime().exec(cmd,null, new File(workDir));
			}
			StreamGobbler stdOut=new StreamGobbler(buffer, process.getInputStream());
			StreamGobbler stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdOut.start();
			stdErr.start();
			
			int timeOut = 60 * 10 ;
			String timeOutStr = pref.getValue(Preference.JDE_RUN_TIMEOUT);
			try {
				timeOut = Integer.parseInt(timeOutStr);
			} catch (NumberFormatException e) {
			}
	
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
		    new BufferChecker().run();
		    finished = true;
		}
		
	}

	class BufferChecker implements Runnable {
		public void run() {
			if (buffer.length() > 0) {
				try {
					String vimCmdCall = "<esc><esc>:FetchResult " + uuid + " "
							+ bufname + "<cr>";
					String[] vimRemoteCmdArray = new String[] { "gvim",
							"--servername", vimServerName, "--remote-send",
							vimCmdCall };
					Runtime.getRuntime().exec(vimRemoteCmdArray);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
}
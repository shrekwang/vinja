package com.github.vinja.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemJob extends Thread {

	
	private int jobId;
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
	private OutputStream stdIn = null;
	

	public SystemJob(int jobId,String cmd,String vimServerName,String cmdShell, 
			String bufname,String workDir,String origCmdLine) {
		this.jobId = jobId;
		this.cmdArray = cmd.split("::");
		this.vimServerName = vimServerName;
		this.uuid = IdGenerator.getUniqueId();
		this.bufname = bufname;
		this.workDir = workDir;
		this.origCmdLine = origCmdLine;
		if (cmdShell !=null && cmdShell.equalsIgnoreCase("true")) {
			runInShell = true;
		}
		BufferStore.put(uuid, buffer);
	}
	
	public void feedInput(String input) {
		try {
			byte[] bytes = input.getBytes();
			stdIn.write(bytes);
			stdIn.flush();
			//feeded string also goes to output
			buffer.append(new String(bytes));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			if (runInShell) {
				String[] newCmdArray = new String[4];
				newCmdArray[0] = "cmd.exe";
				newCmdArray[1] = "/s";
				newCmdArray[2] = "/c";
				newCmdArray[3] = this.origCmdLine;
				/*
				for (int i=0; i<cmdArray.length; i++) {
					newCmdArray[i+2]=cmdArray[i];
				    if (!cmdArray[i].startsWith("\"")) {
                        newCmdArray[i+2]="\"" + cmdArray[i] + "\"";
				    }
				}
				*/
				process = Runtime.getRuntime().exec(newCmdArray,null, new File(workDir));
			} else {
				process = Runtime.getRuntime().exec(cmdArray,null, new File(workDir));
			}
			stdOut=new StreamGobbler(buffer, process.getInputStream());
			stdErr=new StreamGobbler(buffer, process.getErrorStream());
			stdIn = process.getOutputStream();
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
	        exec.scheduleAtFixedRate(new BufferChecker(this.jobId), 1, 100, TimeUnit.MILLISECONDS);
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
				if (stdOut == null || !stdOut.isAlive())  break;
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
		    new BufferChecker(this.jobId).run();
		    SystemJobManager.finishJob(this.jobId);
		}
		
	}

	class BufferChecker implements Runnable {
		private int jobId;
		public BufferChecker(int jobId) {
			this.jobId = jobId;
		}
		public void run() {
			synchronized (buffer) {
				if ( ! (buffer.length() > 0)) return; 
				String[] args = new String[] { uuid,bufname };
				VjdeUtil.callVimFunc(vimServerName, "FetchResult", args);
				SystemJobManager.setLastJobId(jobId);
			}
		}
	}
}
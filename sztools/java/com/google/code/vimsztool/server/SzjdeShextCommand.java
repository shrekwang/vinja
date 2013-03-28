package com.google.code.vimsztool.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.code.vimsztool.util.BufferStore;
import com.google.code.vimsztool.util.VjdeUtil;

public abstract class SzjdeShextCommand extends SzjdeCommand {
	
	private StringWriter strWriter = new StringWriter();
	private StringBuffer buffer = strWriter.getBuffer();
	
	protected ScheduledExecutorService exec = null;
	protected PrintWriter out = new PrintWriter(strWriter);  
	protected String uuid;
	protected String vimServerName;
	protected String bufname ;
	
	public String execute() {
		
		vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
		bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
		
		uuid=UUID.randomUUID().toString();
		BufferStore.put(uuid, buffer);
		exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(new BufferChecker(buffer,uuid), 1, 200, TimeUnit.MILLISECONDS);
        Thread job = createShextJob();
        JobFinishNotifier notifier = new JobFinishNotifier(job); 
        
        job.start();
        notifier.start();
        
        return "";
	}
	
	public abstract Thread createShextJob() ;
	
	public Thread callBackJob () {
		return null;
	}
	
	public abstract String getCmdName() ;
	
	class JobFinishNotifier extends Thread {
		private Thread job = null;
		public JobFinishNotifier(Thread job) {
			this.job = job;
		}
		public void run() {
			try {
				job.join();
			} catch (InterruptedException e) {
				
			}
			
			out.println("");
			out.println("(" + getCmdName() + " finished.)");
			new BufferChecker(buffer,uuid).run();
			exec.shutdown();
			Thread callBackJob = callBackJob();
			if (callBackJob != null) {
				callBackJob.start();
			}
		}
	}
		
	class BufferChecker implements Runnable {
		private StringBuffer buffer;
		private String uuid;
		
		private BufferChecker(StringBuffer buffer,String uuid) {
			this.buffer = buffer;
			this.uuid = uuid;
		}
		public void run() {
			synchronized (buffer) {
				if ( ! (buffer.length() > 0)) return; 
				String[] args = new String[] { uuid,bufname };
				VjdeUtil.callVimFunc(vimServerName, "FetchResult", args);
				
			}
		}
	}
	
	
	

}

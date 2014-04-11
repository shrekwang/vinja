package com.github.vinja.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SystemJobManager {
	
	private static ConcurrentHashMap<Integer, SystemJob> runningJobs = new ConcurrentHashMap<Integer, SystemJob>();
	private static AtomicInteger maxJobId = new AtomicInteger(0);
	private static volatile int lastJob;
	
	public static SystemJob createJob(String cmd,String vimServerName,String cmdShell, 
			String bufname,String workDir,String origCmdLine) {
		int jobId = maxJobId.incrementAndGet();
		SystemJob job =new SystemJob(jobId,cmd,vimServerName,cmdShell,bufname,workDir,origCmdLine); 
		runningJobs.put(jobId, job);
		return job;
	}
	
	public static void finishJob(int jobId) {
		runningJobs.remove(jobId);
	}
	
	public static SystemJob getJob(int jobId) {
		return runningJobs.get(jobId);
	}
	
	public static SystemJob getLastJob() {
		return runningJobs.get(lastJob);
	}
	
	public static void setLastJobId(int jobId) {
		lastJob = jobId;
	}

}

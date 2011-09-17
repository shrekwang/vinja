package com.google.code.vimsztool.server;

import java.util.UUID;

import com.google.code.vimsztool.util.SystemJob;

public class SzjdeSystemCommand extends SzjdeCommand {
	
	public SzjdeSystemCommand(String cmd) {
		this.cmd = cmd;
	}
	public String execute() {
		if (cmd.equals(SzjdeConstants.CMD_RUN_SYS)) {
			String vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
			String cmd = params.get(SzjdeConstants.PARAM_CMD_NAME);
			String cmdShell = params.get(SzjdeConstants.PARAM_RUN_IN_SHELL);
			String bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
			String workDir = params.get(SzjdeConstants.PARAM_WORK_DIR);
			String origCmdLine = params.get("origCmdLine");
			String uuid=UUID.randomUUID().toString();
			SystemJob job = new SystemJob(cmd,vimServerName,cmdShell,uuid,bufname,workDir,origCmdLine);
			job.start();
			return "";
		} else {
			String jobId = params.get(SzjdeConstants.PARAM_JOB_ID);
			SystemJob job = SystemJob.getJob(jobId);
			String result = job.fetchResult();
			return result;
		}
	}

	
}

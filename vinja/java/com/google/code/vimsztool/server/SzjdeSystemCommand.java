package com.google.code.vimsztool.server;

import com.google.code.vimsztool.util.BufferStore;
import com.google.code.vimsztool.util.SystemJob;
import com.google.code.vimsztool.util.SystemJobManager;

public class SzjdeSystemCommand extends SzjdeCommand {
	
	public SzjdeSystemCommand(String cmd) {
		this.cmd = cmd;
	}
	public String execute() {
		String result = "";
		if (cmd.equals(SzjdeConstants.CMD_RUN_SYS)) {
			String vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
			String cmd = params.get(SzjdeConstants.PARAM_CMD_NAME);
			String cmdShell = params.get(SzjdeConstants.PARAM_RUN_IN_SHELL);
			String bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
			String workDir = params.get(SzjdeConstants.PARAM_WORK_DIR);
			String origCmdLine = params.get("origCmdLine");
			SystemJob job = SystemJobManager.createJob(cmd,vimServerName,cmdShell,bufname,workDir,origCmdLine);
			job.start();
		} else if (cmd.equals(SzjdeConstants.CMD_FEED_INPUT)) {
			SystemJob job = SystemJobManager.getLastJob();
			if (job == null ) {
				result = "job isn't running.";
			}
			String input = params.get(SzjdeConstants.PARAM_INPUT_STRING);
			input = input + "\n";
			job.feedInput(input);
		} else {
			String uuid = params.get(SzjdeConstants.PARAM_UUID_ID);
			result = BufferStore.getContent(uuid);
		}
		return result;
	}

	
}

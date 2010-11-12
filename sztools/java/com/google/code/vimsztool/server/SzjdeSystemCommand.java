package com.google.code.vimsztool.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.code.vimsztool.util.VjdeUtil;

public class SzjdeSystemCommand extends SzjdeCommand {
	
	private static Map<String,String> cmdResults = new HashMap<String,String>();
	
	public SzjdeSystemCommand(String cmd) {
		this.cmd = cmd;
	}

	public String execute() {
		if (cmd.equals(SzjdeConstants.CMD_RUN_SYS)) {
			String vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
			String cmdArray = params.get(SzjdeConstants.PARAM_CMD_NAME);
			String cmdShell = params.get(SzjdeConstants.PARAM_RUN_IN_SHELL);
			SystemJob job = new SystemJob(cmdArray,vimServerName,cmdShell);
			job.start();
			return "";
		} else {
			String jobId = params.get(SzjdeConstants.PARAM_JOB_ID);
			String result = cmdResults.get(jobId);
			return result;
		}
	}

	class SystemJob extends Thread {

		private String cmd;
		private String vimServerName;
		private boolean runInShell = false;
		private String uuid;

		public SystemJob(String cmd,String vimServerName,String cmdShell) {
			this.cmd = cmd;
			this.vimServerName = vimServerName;
			if (cmdShell !=null && cmdShell.equalsIgnoreCase("true")) {
				runInShell = true;
			}
			this.uuid=UUID.randomUUID().toString();
		}

		public void run() {
			try {
				String line;
				Process p ;
				if (runInShell) {
					p = Runtime.getRuntime().exec(new String[] {"cmd.exe", "/c",cmd});
				} else {
					p = Runtime.getRuntime().exec(cmd);
				}
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				StringBuilder runResult = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					runResult.append(line).append("\n");
				}
				
				reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					runResult.append(line).append("\n");
				}
	
				reader.close();
				cmdResults.put(uuid, runResult.toString());
			} catch (Exception err) {
				err.printStackTrace();
				cmdResults.put(uuid, VjdeUtil.getExceptionValue(err));
			}
			
			try {
				String vimCmdCall="<esc><esc>:FetchResult "+uuid+"<cr>";
				String[] vimRemoteCmdArray = new String[] {"gvim","--servername",vimServerName,"--remote-send",	vimCmdCall};
				Runtime.getRuntime().exec(vimRemoteCmdArray);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

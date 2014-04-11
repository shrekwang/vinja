package com.google.code.vimsztool.server;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.util.SystemJob;
import com.google.code.vimsztool.util.SystemJobManager;

public class SzjdeClassRunnerCommand extends SzjdeCommand {
	boolean runAsUnitTest;
	
	public SzjdeClassRunnerCommand(boolean runAsUnitTest) {
		this.runAsUnitTest = runAsUnitTest;
	}

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc = getCompilerContext(classPathXml);
		List<String> urls = cc.getFsClassPathUrls();
		StringBuilder cmd = new StringBuilder("java::-cp::");
		for (String url : urls) {
			cmd.append(url).append(File.pathSeparator);
		}

		cmd.append(" ::");
		if (runAsUnitTest) {
			cmd.append("org.junit.runner.JUnitCore::");
		}
		String className = cc.buildClassName(sourceFile);
		cmd.append(className);
		String vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
		String bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
		String origCmdLine = "Run " + className;
		SystemJob job = SystemJobManager.createJob(cmd.toString(),
				vimServerName,"false",bufname,cc.getProjectRoot(),origCmdLine);
		job.start();
		return "";
		
	}

}
package com.google.code.vimsztool.server;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.util.SystemJob;

public class SzjdeClassRunnerCommand extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc = getCompilerContext(classPathXml);
		List<URL> urls = cc.getClassPathUrls();
		StringBuilder cmd = new StringBuilder("java -cp ");
		for (URL url : urls) {
			cmd.append(url.getPath()).append(File.pathSeparator);
		}

		cmd.append(" ");
		cmd.append(cc.buildClassName(sourceFile));
		String uuid=UUID.randomUUID().toString();
		String vimServerName = params.get(SzjdeConstants.PARAM_VIM_SERVER);
		String bufname = params.get(SzjdeConstants.PARAM_BUF_NAME);
		SystemJob job = new SystemJob(cmd.toString(),vimServerName,"false",uuid,bufname,cc.getProjectRoot());
		job.start();
		return "";
		
	}

}
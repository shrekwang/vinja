package com.google.code.vimsztool.server;

import com.google.code.vimsztool.compiler.CompilerContextManager;

public class SzjdeProjectClean extends SzjdeShextCommand {

	@Override
	public Thread createShextJob() {
		Thread job = new Thread() {
			public void run() {
				out.println("wait while cleaning...");
				String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
				CompilerContextManager ccm = CompilerContextManager.getInstnace();
				ccm.reloadCompilerContext(classPathXml);
				out.println("cleaning finised.");
			}
		};
		return job;
	}

	@Override
	public String getCmdName() {
		return "jde project clean";
	}


}
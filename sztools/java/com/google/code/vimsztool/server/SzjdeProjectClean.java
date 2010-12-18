package com.google.code.vimsztool.server;

import com.google.code.vimsztool.compiler.CompilerContextManager;

public class SzjdeProjectClean extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		ccm.reloadCompilerContext(classPathXml);
		return "";
	}

}
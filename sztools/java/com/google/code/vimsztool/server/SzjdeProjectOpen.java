package com.google.code.vimsztool.server;

import java.io.File;

import com.google.code.vimsztool.compiler.CompilerContextManager;

public class SzjdeProjectOpen extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		CompilerContextManager ccm = CompilerContextManager.getInstnace();
		File file = new File(classPathXml);
		
		if (!file.exists()) return "";
		
		ccm.loadCompilerContext(classPathXml);
		return "";
	}

}
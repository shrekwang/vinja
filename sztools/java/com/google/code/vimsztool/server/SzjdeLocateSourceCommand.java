package com.google.code.vimsztool.server;

import com.google.code.vimsztool.compiler.CompilerContext;

public class SzjdeLocateSourceCommand  extends SzjdeCommand {
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String className = params.get(SzjdeConstants.PARAM_CLASS_NAME);
		CompilerContext cc=getCompilerContext(classPathXml);
		String rtlPathName = className.replace(".", "/") + ".java";
		String sourcePath  = cc.findSourceFile(rtlPathName);
		return sourcePath;
	}

}

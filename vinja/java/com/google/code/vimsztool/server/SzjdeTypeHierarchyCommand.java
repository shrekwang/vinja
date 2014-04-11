package com.google.code.vimsztool.server;

import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassMetaInfoManager;
import com.google.code.vimsztool.util.JdeLogger;

public class SzjdeTypeHierarchyCommand extends SzjdeCommand {
	
	private static JdeLogger log = JdeLogger.getLogger("SzjdeTypeHierarchyCommand");
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc=getCompilerContext(classPathXml);
		String className = cc.buildClassName(sourceFile);
		ClassMetaInfoManager cmm = cc.getClassMetaInfoManager();
		List<String> result = cmm.getTypeHierarchy(className);
		StringBuilder sb = new StringBuilder();
		for (String line : result) {
			sb.append(line).append("\n");
		}
		return sb.toString();
	}
	
}

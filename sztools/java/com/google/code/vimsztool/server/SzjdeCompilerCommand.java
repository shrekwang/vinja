package com.google.code.vimsztool.server;

import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.JDTCompiler;

public class SzjdeCompilerCommand extends SzjdeCommand {
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc=getCompilerContext(classPathXml);
		JDTCompiler compiler =new JDTCompiler(cc);
		List<String> problemList=compiler.generateClass(sourceFile);
		if (problemList.size() == 0 ) return "";
		
		StringBuilder sb = new StringBuilder();
		for (String problem : problemList) {
			sb.append(problem);
		}
		return sb.toString();
	}
}

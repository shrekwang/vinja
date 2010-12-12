package com.google.code.vimsztool.server;

import java.io.File;
import java.util.List;

import com.google.code.vimsztool.compiler.CompileResultInfo;
import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.JDTCompiler;
import com.google.code.vimsztool.util.HotSwapUtil;

public class SzjdeCompilerCommand extends SzjdeCommand {
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc=getCompilerContext(classPathXml);
		JDTCompiler compiler =new JDTCompiler(cc);
		CompileResultInfo resultInfo =compiler.generateClass(sourceFile);
		List<String> problemList = resultInfo.getProblemInfoList();
		
		
		HotSwapUtil hotSwapUtil = HotSwapUtil.getInstance();
		if (hotSwapUtil.isEnabled() && ! resultInfo.isError()) {
			try {
				hotSwapClass(resultInfo);
			} catch (Exception e) {
			}
		}
		
		if (problemList.size() == 0 ) return "";
		StringBuilder sb = new StringBuilder();
		for (String problem : problemList) {
			sb.append(problem);
		}
		return sb.toString();
	}
	
	private void hotSwapClass(CompileResultInfo resultInfo) throws Exception {
		
		HotSwapUtil hotSwapUtil = HotSwapUtil.getInstance();
		List<String[]> outputs = resultInfo.getOutputInfo();
		for (String[] names : outputs) {
			String className = names[0];
			String outputPath = names[1];
			hotSwapUtil.replace(new File(outputPath), className);
		}
		
	}
}

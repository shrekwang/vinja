package com.google.code.vimsztool.server;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import com.google.code.vimsztool.compiler.CompileResultInfo;
import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.JDTCompiler;
import com.google.code.vimsztool.debug.BreakpointManager;
import com.google.code.vimsztool.debug.Debugger;
import com.google.code.vimsztool.util.HotSwapUtil;
import com.google.code.vimsztool.util.JdeLogger;

public class SzjdeCompilerCommand extends SzjdeCommand {
	private static Logger log = JdeLogger.getLogger("SzjdeCompilerCommand");
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc=getCompilerContext(classPathXml);
		JDTCompiler compiler =new JDTCompiler(cc);
		String[] allSrcFiles = new String[] {sourceFile};
		if (sourceFile.equals("All")) {
			allSrcFiles = cc.getAllSourceFiles();
		} 
		CompileResultInfo resultInfo =compiler.generateClass(allSrcFiles);
		List<String> problemList = resultInfo.getProblemInfoList();
		
		if (!resultInfo.isError()) {
			hotSwapClass(resultInfo);
		}
		
		if (problemList.size() == 0 ) return "";
		StringBuilder sb = new StringBuilder();
		for (String problem : problemList) {
			sb.append(problem);
		}
		return sb.toString();
	}
	
	private void hotSwapClass(CompileResultInfo resultInfo) {
		
		Debugger debugger = Debugger.getInstance();
		BreakpointManager bpm = BreakpointManager.getInstance();
		List<String[]> outputs = resultInfo.getOutputInfo();
		for (String[] names : outputs) {
			String className = names[0];
			String outputPath = names[1];
			HotSwapUtil.replace(debugger, new File(outputPath), className);
			bpm.tryResetBreakpointRequest(className);
		}
		
	}
}

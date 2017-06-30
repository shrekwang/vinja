package com.github.vinja.server;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.parser.VinjaJavaSourceSearcher;
import com.github.vinja.parser.VinjaSourceLoadPriority;

public class SzjdePreloadCommand extends SzjdeCommand {
	

	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		CompilerContext cc = getCompilerContext(classPathXml);
		VinjaJavaSourceSearcher.loadJavaSourceSearcher(sourceFile, cc, VinjaJavaSourceSearcher.NameType.FILE,true, VinjaSourceLoadPriority.HIGH);
		return "";
		
	}

}
package com.github.vinja.server;

import static com.github.vinja.server.SzjdeConstants.PARAM_CLASSPATHXML;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.parser.IJavaSourceSearcher;
import com.github.vinja.parser.LocationInfo;
import com.github.vinja.parser.VinjaJavaSourceSearcher;


public class SzjdeSearchDefLocation extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
	    String sourceType = params.get(SzjdeConstants.PARAM_SOURCE_TYPE);
	    
		int row = Integer.parseInt(params.get("row"));
		int col = Integer.parseInt(params.get("col"));
		
		CompilerContext ctx = getCompilerContext(classPathXml);
		IJavaSourceSearcher searcher = VinjaJavaSourceSearcher.createSearcher(sourceFile,ctx);
		LocationInfo info = searcher.searchDefLocation(row,col,sourceType);
		
		//can't find the location
		if (info == null || info.getFilePath() == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(info.getFilePath()).append(";");
		sb.append(info.getLine()).append(";");
		sb.append(info.getCol());
		return sb.toString();
	}
	
	
	

}

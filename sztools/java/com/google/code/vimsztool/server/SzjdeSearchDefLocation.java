package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.parser.JavaSourceSearcher;
import com.google.code.vimsztool.parser.LocationInfo;


public class SzjdeSearchDefLocation extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		int row = Integer.parseInt(params.get("row"));
		int col = Integer.parseInt(params.get("col"));
		
		CompilerContext ctx = getCompilerContext(classPathXml);
		JavaSourceSearcher searcher = new JavaSourceSearcher(sourceFile,ctx);
		LocationInfo info = searcher.searchDefLocation(row,col);
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

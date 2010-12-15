package com.google.code.vimsztool.server;

import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.PackageInfo;

public class SzjdeAutoImportCommand extends SzjdeCommand  {

	@Override
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String currentPkg = params.get(SzjdeConstants.PARAM_PKG_NAME);
		String[] varNames = params.get(SzjdeConstants.PARAM_VAR_NAMES).split(",");
		CompilerContext ctx = getCompilerContext(classPathXml);
		PackageInfo packageInfo =ctx.getPackageInfo();
		
		StringBuilder sb = new StringBuilder();
		for (String varName : varNames ) {
			varName = varName.trim();
			if (isKeyword(varName)) continue;
			StringBuilder tmpSb = new StringBuilder();
			
			List<String> binClassNames=packageInfo.findPackage(varName);
			if (binClassNames.size() == 0) continue;
			boolean noNeedImport = false;
			for (String binClassName : binClassNames) {
				String pkgName = binClassName.substring(0,binClassName.lastIndexOf("."));
				if ( pkgName.equals("java.lang")  || pkgName.equals(currentPkg)) {
					noNeedImport =true;
					break;
				}
				tmpSb.append(binClassName).append(";");
			}
			if (noNeedImport) continue;
			sb.append(tmpSb.toString()).append("\n");
		}
		
		return sb.toString();
	}
	
	public boolean isKeyword(String name) {
		
		String[] keywordList = new String[] {
				  "synchronized","int","abstract","float",
				  "private","const","char","interface",
				  "boolean","static","if","strictfp",
				  "for","enum","goto","while",
				  "long","class","case","finally",
				  "protected","extends","new","native",
				  "public","do","return","void","else",
				  "break","transient","assert",
				  "import","catch","instanceof",
				  "byte","super","throw","volatile",
				  "implements","short","package","default",
				  "double","final","try","this",
				  "switch","continue","throws" };
		
		for (String keyword : keywordList) {
			if (keyword.equals(name)) {
				return true;
			}
		}
		return false;
	}

}

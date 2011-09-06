package com.google.code.vimsztool.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.MemberReferenceFinder;
import com.google.code.vimsztool.omni.ReferenceLocation;

public class SzjdeSearchReferenceCommand extends SzjdeCommand {
	
	private static final String SEPERATOR = "::";
	
	public SzjdeSearchReferenceCommand() {
	}
	
	public String execute() {
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String srcPath = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		String memberDesc = params.get(SzjdeConstants.PARAM_MEMBER_DESC);
		File file = new File(classPathXml);
		if (file.isDirectory()) return "";
		CompilerContext ctx=getCompilerContext(classPathXml);
		String targetClass= ctx.buildClassName(srcPath);
		targetClass = targetClass.replace(".", "/");
		String result = search(ctx,targetClass,memberDesc);
		return result;
	}
	
	public String search(CompilerContext ctx , String targetClass, String memberDesc) {
		 MemberReferenceFinder app = new MemberReferenceFinder();
         try {
	         app.findCallingMethodInDir(ctx.getOutputDir(), targetClass,memberDesc);
         } catch (Exception e) {
        	 e.printStackTrace();
         }
         StringBuilder sb = new StringBuilder();
         for (ReferenceLocation loc : app.getReferenceLocations() ) {
        	 sb.append(getSourcePath(ctx,loc.className)).append(SEPERATOR);
        	 sb.append(loc.line).append(SEPERATOR);
        	 sb.append(getSourceLine(ctx,loc.className,loc.line)).append("\n");
         }
         return sb.toString();
	}
	
	public String getSourcePath(CompilerContext ctx ,String className) {
		String rtlPathName = className.replace(".", "/") + ".java";
		String sourcePath = ctx.findSourceFile(rtlPathName);
		return sourcePath;
	}
	
	public String getSourceLine(CompilerContext ctx, String className, int line) {
		String sourcePath = getSourcePath(ctx,className);
	    FileReader fr = null;
		try {
		    fr = new FileReader(sourcePath);
	        BufferedReader br = new BufferedReader(fr);
	        String result = "";
	        int i = 0;
	        while (true) {
	        	i++;
	        	result = br.readLine();
	        	if (result == null || i==line ) break;
	        }
	        return result;
		} catch (IOException e) {
			
		} finally {
			if (fr !=null) try { fr.close(); } catch (Exception e) {}
		}
		return "";
	}

	
}

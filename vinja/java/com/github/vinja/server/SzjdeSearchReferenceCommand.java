package com.github.vinja.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.omni.MemberReferenceFinder;
import com.github.vinja.omni.ReferenceLocation;

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
        	 sb.append(getSourcePath(ctx,loc.className,loc.source)).append(SEPERATOR);
        	 sb.append(loc.line).append(SEPERATOR);
        	 sb.append(getSourceLine(ctx,loc.className,loc.source,loc.line)).append("\n");
         }
         return sb.toString();
	}
	
	public String getSourcePath(CompilerContext ctx ,String className,String sourceName) {
		String rtlPathName = sourceName;
		if (className.indexOf("/") > 0 ) {
			rtlPathName = className.substring(0,className.lastIndexOf("/")+1) + sourceName;
		}
		
		String sourcePath = ctx.findSourceFile(rtlPathName);
		return sourcePath;
	}
	
	public String getSourceLine(CompilerContext ctx, String className, String sourceName, int line) {
		String sourcePath = getSourcePath(ctx,className,sourceName);
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

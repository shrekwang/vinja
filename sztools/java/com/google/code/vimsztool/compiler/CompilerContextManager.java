package com.google.code.vimsztool.compiler;

import java.util.HashMap;

public class CompilerContextManager {
	
	private static HashMap<String, CompilerContext> ctxCache = new HashMap<String, CompilerContext>();
	private static CompilerContextManager instance = new CompilerContextManager();

	private CompilerContextManager () { }
	
	public static CompilerContextManager  getInstnace() {
		return instance;
	}
	
	public CompilerContext getCompilerContext(String classPathXml) {
		CompilerContext ctx=ctxCache.get(classPathXml);
		if (ctx ==null) {
			ctx=new CompilerContext(classPathXml);
			ctxCache.put(classPathXml, ctx);
		}
		return ctx;
	}
	
	public void reloadCompilerContext(String classPathXml) {
		CompilerContext ctx=new CompilerContext(classPathXml);
		ctxCache.put(classPathXml, ctx);
	}

}

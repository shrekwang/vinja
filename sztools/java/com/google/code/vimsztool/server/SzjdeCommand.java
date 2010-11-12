package com.google.code.vimsztool.server;

import java.util.HashMap;
import java.util.Map;

import com.google.code.vimsztool.compiler.CompilerContext;

public class SzjdeCommand {

	protected String cmd;
	protected Map<String,String> params;
	protected static HashMap<String, CompilerContext> ctxCache = new HashMap<String, CompilerContext>();
	
	public CompilerContext getCompilerContext(String classPathXml) {
		CompilerContext ctx=ctxCache.get(classPathXml);
		if (ctx ==null) {
			ctx=new CompilerContext(classPathXml);
			ctxCache.put(classPathXml, ctx);
		}
		return ctx;
	}


	
	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public String execute() { return null; }
}

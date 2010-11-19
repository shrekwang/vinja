package com.google.code.vimsztool.server;

import java.util.Map;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.CompilerContextManager;

public class SzjdeCommand {

	protected String cmd;
	protected Map<String,String> params;
	protected CompilerContextManager ccm = CompilerContextManager.getInstnace();
	
	public CompilerContext getCompilerContext(String classPathXml) {
		return ccm.getCompilerContext(classPathXml);
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

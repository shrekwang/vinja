package com.google.code.vimsztool.parser;

import org.antlr.runtime.tree.CommonTree;

public class ParseResult {
	
	private boolean hasError;
	private String errorMsg;
	private CommonTree tree;
	
	public boolean hasError() {
		return hasError;
	}
	public void setError(boolean hasError) {
		this.hasError = hasError;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public CommonTree getTree() {
		return tree;
	}
	public void setTree(CommonTree tree) {
		this.tree = tree;
	}

}

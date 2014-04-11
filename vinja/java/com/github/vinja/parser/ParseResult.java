package com.github.vinja.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

public class ParseResult {
	
	private boolean hasError;
	private String errorMsg;
	
	private List<String> expList;
	private List<CommonTree> treeList;
	
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
		return treeList.get(0);
	}
	
	public void setTree(CommonTree tree) {
		this.treeList = new ArrayList<CommonTree>();
		treeList.add(tree);
	}
	
	public void setExp(String exp) {
		this.expList = new ArrayList<String>();
		expList.add(exp);
	}
	
	public String getExp(){
		return this.expList.get(0);
	}

	public List<CommonTree> getTreeList() {
		return treeList;
	}
	public void setTreeList(List<CommonTree> treeList) {
		this.treeList = treeList;
	}
	public List<String> getExpList() {
		return expList;
	}
	public void setExpList(List<String> expList) {
		this.expList = expList;
	}
	
	public boolean isMultipleExp() {
		return this.treeList.size() > 0 ;
	}

}

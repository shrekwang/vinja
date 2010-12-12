package com.google.code.vimsztool.compiler;

import java.util.ArrayList;
import java.util.List;

public class CompileResultInfo {
	
	private boolean error;
	private List<String> problemInfoList = new ArrayList<String>();
	private List<String[]> outputClassList = new ArrayList<String[]>();
	
	public void addProblemInfo(String msg) {
		problemInfoList.add(msg);
	}
	public void addOutputInfo(String className, String outputPath) {
		outputClassList.add(new String[]{className,outputPath});
	}
	
	public List<String> getProblemInfoList() {
		return this.problemInfoList;
	}
	
	public List<String[]> getOutputInfo() {
		return this.outputClassList;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public boolean isError() {
		return error;
	}
	
	
}

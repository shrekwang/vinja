package com.google.code.vimsztool.omni;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassInfo {
	private String name;
	private String superName;
	private String[] interfaces;
	private Set<String> subNames = new HashSet<String>();
	private List<Integer> lineNums = new ArrayList<Integer>();
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSuperName() {
		return superName;
	}
	public void setSuperName(String superName) {
		this.superName = superName;
	}
	public String[] getInterfaces() {
		return interfaces;
	}
	public void setInterfaces(String[] interfaces) {
		this.interfaces = interfaces;
	}
	public Set<String> getSubNames() {
		return subNames;
	}
	public void addSubName(String subName) {
		this.subNames.add(subName);
		
	}
	public List<Integer> getLineNums() {
		return lineNums;
	}
	public void addLineNum(int lineNum) {
		this.lineNums.add(lineNum);
	}
	
	
}

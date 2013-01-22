package com.google.code.vimsztool.omni;

public class MemberInfo {
	
	public static final String TYPE_METHOD="method"; 
	public static final String TYPE_FIELD="field"; 
	public static final String TYPE_CLASS="class"; 
	public static final String TYPE_CONSTRUCTOR="constructor"; 
	
	private String memberType;
	
	private String name;
	private String returnType="";
	private String params="";
	private String exceptions="";
	private String modifiers = "";
		
	
	public String getMemberType() {
		return memberType;
	}
	public void setMemberType(String memberType) {
		this.memberType = memberType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	public String getReturnType() {
		return returnType;
	}
	public void setParams(String params) {
		this.params = params;
	}
	public String getParams() {
		return params;
	}
	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
	}
	public String getExceptions() {
		return exceptions;
	}
	public void setModifiers(String modifiers) {
		this.modifiers = modifiers;
	}
	public String getModifiers() {
		return modifiers;
	}
	public String getFullDecleration() {
		
		String result= modifiers + " " + returnType +" "+ name ;
		if (!memberType.equals(TYPE_FIELD)) {
			result = result + "("+params+")";
			if (exceptions.length() > 1 ) {
				result = result + " throws " + exceptions;
			}
		}
		result = result + ";";
		return result;
	}
	
	

}

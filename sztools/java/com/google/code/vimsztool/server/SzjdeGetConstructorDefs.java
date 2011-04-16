package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;

import java.util.ArrayList;

import com.google.code.vimsztool.omni.ClassInfoUtil;
import com.google.code.vimsztool.omni.MemberInfo;


public class SzjdeGetConstructorDefs extends SzjdeCommand {

	@SuppressWarnings("unchecked")
	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String[] classNameList = params.get("classnames").split(",");
		
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfoUtil.getExistedClass(classPathXml, classNameList, sourceFile);
		
		if (aClass == null) return "";
		
	    ArrayList<MemberInfo> memberInfos =	ClassInfoUtil.getConstructorInfo(aClass);
		StringBuilder sb=new StringBuilder();
		for (MemberInfo member : memberInfos) {
			sb.append(member.getReturnType()).append(" ");
			sb.append(member.getName()).append("(");
			sb.append(member.getParams()).append(") ");
			if (!member.getExceptions().trim().equals("")) {
				sb.append(" throws ");
				sb.append(member.getExceptions());
			} 
			sb.append("\n");
		}
		return sb.toString();
	}
	

}

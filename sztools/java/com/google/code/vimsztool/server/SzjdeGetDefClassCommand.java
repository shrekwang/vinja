package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_EXP_TOKENS;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_MEMBER_NAME;

import com.google.code.vimsztool.omni.ClassInfo;
import com.google.code.vimsztool.omni.JavaExpUtil;
import com.google.code.vimsztool.util.ModifierFilter;


public class SzjdeGetDefClassCommand extends SzjdeCommand {

	@SuppressWarnings("unchecked")
	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
		String memberName = params.get(PARAM_MEMBER_NAME);
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfo.getExistedClass(classPathXml, classNameList, sourceFile);
		if (aClass == null) return "";
		ModifierFilter filter = new ModifierFilter(false,true);
		aClass = JavaExpUtil.parseExpResultType(tokens, aClass,filter);
		if (aClass == null) return "";
		
		String memberType = "field";
		if (memberName.endsWith("()")) {
			memberType = "method";
			memberName = memberName.substring(0,memberName.indexOf("("));
		}
		aClass = JavaExpUtil.searchMemberInHierarchy(aClass, memberName ,memberType ,filter,true);
		if (aClass == null) return "";
		return aClass.getName();
	}
	
	

}

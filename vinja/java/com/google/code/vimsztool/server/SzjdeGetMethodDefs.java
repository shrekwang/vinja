package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_EXP_TOKENS;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_MEMBER_NAME;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassInfoUtil;
import com.google.code.vimsztool.omni.JavaExpUtil;
import com.google.code.vimsztool.omni.MemberInfo;
import com.google.code.vimsztool.util.MemberInfoResolver;
import com.google.code.vimsztool.util.ModifierFilter;


public class SzjdeGetMethodDefs extends SzjdeCommand {

	@SuppressWarnings("all")
	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
		String methodName = params.get(PARAM_MEMBER_NAME);
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfoUtil.getExistedClass(classPathXml, classNameList, sourceFile);
		if (aClass == null) return "";
		ModifierFilter filter = new ModifierFilter(false,true);
		aClass = JavaExpUtil.parseExpResultType(tokens, aClass,filter);
		if (aClass == null) return "";
		return this.getAllMember(aClass,methodName, classPathXml);
	}
	
	@SuppressWarnings("all")
	public String getAllMember(Class aClass,String methodName, String classPathXml) {
		
		List<MemberInfo> memberInfos=new ArrayList<MemberInfo>();
		LinkedList<Class> classList = ClassInfoUtil.getAllSuperClass(aClass);
		
		CompilerContext ctx = this.getCompilerContext(classPathXml);
	
		for (Class cls : classList) {
			
			List<MemberInfo> tmpInfoList = MemberInfoResolver.resolveMemberInfo(ctx, cls);
			
			if (tmpInfoList == null) continue;
			for (MemberInfo tmpMember : tmpInfoList) {
				boolean added = false;
				if (! tmpMember.getName().equals(methodName)) continue;
				for (MemberInfo member : memberInfos) {
					if ( member.getParams().equals(tmpMember.getParams()) ) {
						added = true;
					}
				}
				if (! added ) {
					memberInfos.add(tmpMember);
				}
			}
		}
		
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

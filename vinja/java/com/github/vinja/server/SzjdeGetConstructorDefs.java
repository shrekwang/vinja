package com.github.vinja.server;

import static com.github.vinja.server.SzjdeConstants.PARAM_CLASSPATHXML;

import java.util.List;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.omni.ClassInfoUtil;
import com.github.vinja.omni.MemberInfo;
import com.github.vinja.util.MemberInfoResolver;


public class SzjdeGetConstructorDefs extends SzjdeCommand {

	@SuppressWarnings("all")
	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String[] classNameList = params.get("classnames").split(",");
		
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfoUtil.getExistedClass(classPathXml, classNameList, sourceFile);
		
		if (aClass == null) return "";
		CompilerContext ctx = this.getCompilerContext(classPathXml);
	    List<MemberInfo> memberInfos =	MemberInfoResolver.resolveConstructorInfo(ctx, aClass);
		
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

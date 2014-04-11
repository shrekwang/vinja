package com.github.vinja.server;

import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_CLASS;
import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_CLASSMEMBER;
import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_CONSTRUCTOR;
import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_INHERITMEMBER;
import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_OBJECTMEMBER;
import static com.github.vinja.server.SzjdeConstants.CPT_TYPE_PACKAGE;
import static com.github.vinja.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.github.vinja.server.SzjdeConstants.PARAM_CLASS_NAME;
import static com.github.vinja.server.SzjdeConstants.PARAM_CPT_TYPE;
import static com.github.vinja.server.SzjdeConstants.PARAM_EXP_TOKENS;
import static com.github.vinja.server.SzjdeConstants.PARAM_IGNORE_CASE;
import static com.github.vinja.server.SzjdeConstants.PARAM_WITH_LOCATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.compiler.ReflectAbleClassLoader;
import com.github.vinja.omni.ClassInfoUtil;
import com.github.vinja.omni.JavaExpUtil;
import com.github.vinja.omni.MemberInfo;
import com.github.vinja.omni.PackageInfo;
import com.github.vinja.util.ClassNameComparator;
import com.github.vinja.util.MemberInfoResolver;
import com.github.vinja.util.ModifierFilter;


public class SzjdeCompletionCommand extends SzjdeCommand {
	
	private String classPathXml;

	public String execute() {
		this.classPathXml = params.get(PARAM_CLASSPATHXML);
		String completionType = params.get(PARAM_CPT_TYPE);
		if (completionType.equals(CPT_TYPE_PACKAGE)) {
			String pkgname = params.get("pkgname");
			return completePackage(classPathXml, pkgname);
		} else if (completionType.equals(CPT_TYPE_CLASSMEMBER)
				|| completionType.equals(CPT_TYPE_CONSTRUCTOR)
				|| completionType.equals(CPT_TYPE_INHERITMEMBER)
				|| completionType.equals(CPT_TYPE_OBJECTMEMBER) ) {
			return completeMember(classPathXml, completionType);
		} else if (completionType.equals(CPT_TYPE_CLASS)){
			String nameStart = params.get(PARAM_CLASS_NAME);
			String ignoreCaseParam = params.get(PARAM_IGNORE_CASE);
			String withLocationParam = params.get(PARAM_WITH_LOCATION);
			
			
			boolean ignoreCase = false;
			if (ignoreCaseParam != null && ignoreCaseParam.equals("true")) {
				ignoreCase = true;
			}
			boolean withLoc = false;
			if (withLocationParam != null && withLocationParam.equals("true")) {
				withLoc = true;
			}
			return completeClass(classPathXml,nameStart,ignoreCase,withLoc);
		}
		return "";
	}
	public String completeClass(String classPathXml, String nameStart,
			boolean ignoreCase, boolean withLoc) {
		if (classPathXml ==null || nameStart == null) return "";
		CompilerContext ctx = getCompilerContext(classPathXml);
		PackageInfo packageInfo = ctx.getPackageInfo();
		List<String> classNameList = null;
		if (nameStart.length() < 2) {
			return "";
		}
		if (nameStart.indexOf(".") > -1) {
			classNameList=packageInfo.findClassByQualifiedName(nameStart,ignoreCase,withLoc);
		} else {
			classNameList=packageInfo.findClass(nameStart,ignoreCase,withLoc);
		}
		Collections.sort(classNameList, new ClassNameComparator());
		StringBuilder sb=new StringBuilder();
		for (String name : classNameList) {
			if (name.indexOf("$") > -1 ) continue;
			sb.append(name).append("\n");
		}
		return sb.toString();
	}
	
	
	@SuppressWarnings("rawtypes")
	public String completeMember(String classPathXml, String completionType) {
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfoUtil.getExistedClass(classPathXml, classNameList, sourceFile);
		if (aClass == null) return "";
		boolean acceptPrctMember = false;
		if (completionType.equals(CPT_TYPE_INHERITMEMBER)) {
			acceptPrctMember=true;
		}
		ModifierFilter filter = new ModifierFilter(false,acceptPrctMember);
		aClass = JavaExpUtil.parseExpResultType(tokens, aClass,filter);
		
		if (aClass == null) return "";
		boolean hasDotExp = false;
		if (tokens.length > 1 ) {
			hasDotExp = true;
		}
		return getAllMember(aClass,completionType,hasDotExp);
	}
	
	
	
	
	@SuppressWarnings("rawtypes")
	public String getAllMember(Class aClass,String completionType,boolean hasDotExp) {
		List<MemberInfo> memberInfos=new ArrayList<MemberInfo>();
		LinkedList<Class> classList = ClassInfoUtil.getAllSuperClass(aClass);
		
		CompilerContext ctx = getCompilerContext(this.classPathXml);
	
		for (Class cls : classList) {
			if (cls.getName().equals("java.lang.Enum")) continue;
			List<MemberInfo> tmpInfoList = MemberInfoResolver.resolveMemberInfo(ctx, cls, hasDotExp, completionType);
			if (tmpInfoList == null) continue;
			for (MemberInfo tmpMember : tmpInfoList) {
				boolean added = false;
				for (MemberInfo member : memberInfos) {
					if ( member.getName().equals(tmpMember.getName() )
							&& member.getParams().equals(tmpMember.getParams()) ) {
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
			sb.append(member.getMemberType()).append(":");
			sb.append(member.getName()).append(":");
			sb.append(member.getParams()).append(":");
			sb.append(member.getReturnType()).append(":");
			sb.append(member.getExceptions()).append("\n");
		}
		return sb.toString();
	}
	
	
	
	
	public String completePackage(String classPathXml, String pkgname) {
		CompilerContext ctx = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		PackageInfo packageInfo = ctx.getPackageInfo();
		List<String> subNames=packageInfo.getClassesForPackage(pkgname, classLoader);
		StringBuilder sb=new StringBuilder();
		for (String name : subNames) {
			sb.append(name).append("\n");
		}
		return sb.toString();
		
	}

}

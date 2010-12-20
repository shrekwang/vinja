package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CLASS;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CLASSMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CONSTRUCTOR;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_INHERITMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_OBJECTMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_PACKAGE;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASS_NAME;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CPT_TYPE;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_EXP_TOKENS;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.ReflectAbleClassLoader;
import com.google.code.vimsztool.omni.ClassInfo;
import com.google.code.vimsztool.omni.MemberInfo;
import com.google.code.vimsztool.omni.PackageInfo;


public class SzjdeCompletionCommand extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
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
			return completeClass(classPathXml,nameStart);
		}
		return "";
	}
	public String completeClass(String classPathXml, String nameStart) {
		if (classPathXml ==null || nameStart == null) return "";
		CompilerContext ctx = getCompilerContext(classPathXml);
		PackageInfo packageInfo = ctx.getPackageInfo();
		List<String> classNameList=packageInfo.findClass(nameStart);
		StringBuilder sb=new StringBuilder();
		for (String name : classNameList) {
			sb.append(name).append("\n");
		}
		return sb.toString();
	}
	
	
	@SuppressWarnings("unchecked")
	public String completeMember(String classPathXml, String completionType) {
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = getExistedClass(classPathXml, classNameList, sourceFile);
		if (aClass == null) return "";
		aClass = this.parseExpResultType(tokens, aClass,completionType);
		if (aClass == null) return "";
		boolean hasDotExp = false;
		if (tokens.length > 1 ) {
			hasDotExp = true;
		}
		return getAllMember(aClass,completionType,hasDotExp);
	}
	
	@SuppressWarnings("unchecked")
	private Class parseExpResultType(String[] tokens, Class aClass,String completionType) {
		if (tokens == null || tokens.length == 0) return aClass;
		boolean self = false;
		if (completionType.equals(CPT_TYPE_INHERITMEMBER)) self=true;

		String memberType = "";
		for (String token : tokens) {
			if (token.trim().equals("") || token.equals(".") ) continue;
			if (token.indexOf("(") > 0) {
				memberType = "method";
				token=token.substring(0, token.indexOf("("));
			} else {
				memberType = "field";
			}
			aClass = searchMemberTypeInHierarchy(aClass, token, memberType, self);
			self = false;
			if (aClass == null) return null;
		}
		return aClass;
	}
	
	@SuppressWarnings("unchecked")
	private Class searchMemberTypeInHierarchy(Class aClass,String memberName,String memberType,boolean self) {
		Class fClass = null;
		boolean foundField = false;
		while (true) {
			Member[] members = null;
			if (memberType.equals("field")) {
				 members = aClass.getDeclaredFields();
			} else {
				members = aClass.getDeclaredMethods();
			}
			for (Member tf : members) {
				int mod = tf.getModifiers();
				if (Modifier.isPublic(mod) || (self && Modifier.isProtected(mod)) ) {
					if (tf.getName().equals(memberName)) {
						if (memberType.equals("field")) {
							fClass = ((Field)tf).getType();
						} else {
							fClass = ((Method)tf).getReturnType();
						}
						foundField = true;
						break;
					}
				}
			}
			if (foundField) break;
			aClass = aClass.getSuperclass();
			if ( aClass ==null || aClass.getName().equals("java.lang.Object") ) {
				break;
			}
		}
		return fClass;
	}
	
	@SuppressWarnings("unchecked")
	private Class getExistedClass(String classPathXml , String[] classNameList,String sourceFile) {
		CompilerContext ctx = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		Class aClass = null;
		for (String className : classNameList) {
			if (className.equals("this") && sourceFile !=null ) {
				className = ctx.buildClassName(sourceFile);
			}
			try {
				aClass = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					aClass = classLoader.loadClass("java.lang."+className);
				} catch (ClassNotFoundException e2) { }
			}
			if (aClass != null) break;
		}
		return aClass;
	}
	
	@SuppressWarnings("unchecked")
	public String getAllMember(Class aClass,String completionType,boolean hasDotExp) {
		ClassInfo classInfo = new ClassInfo();
		List<MemberInfo> memberInfos=new ArrayList<MemberInfo>();
		LinkedList<Class> classList = new LinkedList<Class>();
		if (aClass.isInterface()) {
			classList.add(aClass);
			for (Class tmpIntf : aClass.getInterfaces()) {
				classList.add(tmpIntf);
			}
		} else {
			Class tmpClass =  aClass;
			while (true) {
				classList.add(tmpClass);
				for (Class tmpIntf : tmpClass.getInterfaces()) {
					classList.add(tmpIntf);
				}
				tmpClass =  tmpClass.getSuperclass();
				if (tmpClass == null) break;
				if (tmpClass.getName().equals("java.lang.Object")) break;
			}
		}
		for (Class cls : classList) {
			List<MemberInfo> tmpInfoList = null;
			if (completionType.equals(CPT_TYPE_OBJECTMEMBER) || hasDotExp ) {
				tmpInfoList=classInfo.getMemberInfo(cls,false,false);
			} else if (completionType.equals(CPT_TYPE_CLASSMEMBER)){
				tmpInfoList=classInfo.getMemberInfo(cls,true,false);
			} else if (completionType.equals(CPT_TYPE_CONSTRUCTOR)){
				tmpInfoList=classInfo.getConstructorInfo(cls);
			} else if (completionType.equals(CPT_TYPE_INHERITMEMBER)){
				tmpInfoList=classInfo.getMemberInfo(cls,false,true);
			}
			if (tmpInfoList == null) continue;
			memberInfos.addAll(tmpInfoList);
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

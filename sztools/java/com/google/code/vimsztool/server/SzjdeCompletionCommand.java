package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CLASS;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CLASSMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CONSTRUCTOR;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_OBJECTMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_PACKAGE;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_SUPER_FIELD_MEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASS_NAME;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CPT_TYPE;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_EXP_TOKENS;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_SUPER_CLASS;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_VAR_NAMES;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
				|| completionType.equals(CPT_TYPE_OBJECTMEMBER) ) {
			return completeMember(classPathXml, completionType);
		} else if (completionType.equals(CPT_TYPE_SUPER_FIELD_MEMBER)) {
			String superClass =params.get(PARAM_SUPER_CLASS);
			String field = params.get(PARAM_VAR_NAMES);
			return completeSuperFieldMember(classPathXml, superClass, field);
		} else if (completionType.equals(CPT_TYPE_CLASS)){
			String nameStart = params.get(PARAM_CLASS_NAME);
			return completeClass(classPathXml,nameStart);
		}
		return "";
	}
	public String completeClass(String classPathXml, String nameStart) {
		if (classPathXml ==null || nameStart == null) return "";
		CompilerContext ctx = getCompilerContext(classPathXml);
		List<String> classNameList=PackageInfo.findClass(nameStart);
		StringBuilder sb=new StringBuilder();
		for (String name : classNameList) {
			sb.append(name).append("\n");
		}
		return sb.toString();
	}
	
	public String completeSuperFieldMember(String classPathXml, String superClass,
			String field) {
		String[] classNameList = superClass.split(",");
		CompilerContext ctx = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		Class aClass = null;
		for (String className : classNameList) {
			try {
				aClass = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
			}
			if (aClass != null)
				break;
		}
		if (aClass == null)
			return "";
		Class fClass = null;
		Field[] fields = aClass.getDeclaredFields();
		for (Field tf : fields) {
			int mod = tf.getModifiers();
			if (Modifier.isPublic(mod) || Modifier.isProtected(mod)) {
				if (tf.getName().equals(field)) {
					fClass = tf.getType();
				}
			}

		}
		if (fClass == null) return "";
		ClassInfo classInfo = new ClassInfo();
		List<MemberInfo> memberInfos=null;
		memberInfos = classInfo.getMemberInfo(fClass, false);
		if (memberInfos == null) return "";
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
	
	public String completeMember(String classPathXml, String completionType) {
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
		
		CompilerContext ctx = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = ctx.getClassLoader();
		Class aClass = null;
		for (String className : classNameList) {
			try {
				aClass = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					aClass = classLoader.loadClass("java.lang."+className);
				} catch (ClassNotFoundException e2) { }
			}
			if (aClass != null) break;
		}
		if (aClass == null) return "";
		boolean hasDotExp = false;
		if (tokens.length > 0) {
			for (String token : tokens) {
				if (token.equals(".")) continue;
				if (token.indexOf("(") > 0) {
					Method[] methods=aClass.getMethods();
					for (Method method : methods) {
						if (method.getName().equals(token.substring(0,token.indexOf("(")))){
							aClass = method.getReturnType();
							hasDotExp = true;
							break;
						}
					}
				} else {
					try {
						aClass=aClass.getDeclaredField(token).getType();
						hasDotExp = true;
						break;
					} catch (NoSuchFieldException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		ClassInfo classInfo = new ClassInfo();
		List<MemberInfo> memberInfos=null;
		if (completionType.equals(CPT_TYPE_OBJECTMEMBER) || hasDotExp ) {
			memberInfos=classInfo.getMemberInfo(aClass,false);
		} else if (completionType.equals(CPT_TYPE_CLASSMEMBER)){
			memberInfos=classInfo.getMemberInfo(aClass,true);
		} else if (completionType.equals(CPT_TYPE_CONSTRUCTOR)){
			memberInfos=classInfo.getConstructorInfo(aClass);
		}
		if (memberInfos == null) return "";
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
		List<String> subNames=PackageInfo.getClassesForPackage(pkgname, classLoader);
		StringBuilder sb=new StringBuilder();
		for (String name : subNames) {
			sb.append(name).append("\n");
		}
		return sb.toString();
		
	}

}

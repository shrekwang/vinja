package com.google.code.vimsztool.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.compiler.ReflectAbleClassLoader;
import com.google.code.vimsztool.omni.ClassInfoUtil;
import com.google.code.vimsztool.omni.MemberInfo;

public class SzjdeOverideMethodsCommand extends SzjdeCommand {

	Set<String> mandatoryMethods = new HashSet<String>();
	Set<String> optionalMethods = new HashSet<String>();
	
	
	@SuppressWarnings("unchecked")
	public String execute() {
		
		String classPathXml = params.get(SzjdeConstants.PARAM_CLASSPATHXML);
		String varNamesStr = params.get(SzjdeConstants.PARAM_VAR_NAMES);
		CompilerContext cc = getCompilerContext(classPathXml);
		ReflectAbleClassLoader classLoader = cc.getClassLoader();
		String[] varNames = varNamesStr.split(",");
		StringBuilder sb=new StringBuilder();
		LinkedList<Class> superClassList = new LinkedList<Class>();
		for (String name : varNames ) {
			Class aClass = null;
			try {
				aClass = classLoader.loadClass(name);
			} catch (ClassNotFoundException e) {
			}
			if (aClass == null ) continue;
			Class tmpClass = aClass;
			while (true) {
				if (tmpClass == null) break;
				superClassList.addFirst(tmpClass);
				Class[] itfs=tmpClass.getInterfaces();
				for (Class itf : itfs ) {
					superClassList.addFirst(itf);
				}
				tmpClass=tmpClass.getSuperclass();
			}
		}
		
		sb.append("Mandatory Methods:\n\n");
		for (Class aClass : superClassList) {
			List<String> methods = addMethods(aClass,true);
			if (methods.size() == 0) continue;
			sb.append("  ").append(aClass.getName()).append("\n");
			for (String method : methods) {
				sb.append("    ").append(method).append("\n");
			}
		}
		sb.append("\n");
		sb.append("Optional Methods:\n\n");
		for (Class aClass : superClassList) {
			List<String> methods = addMethods(aClass,false);
			if (methods.size() == 0) continue;
			sb.append("  ").append(aClass.getName()).append("\n");
			for (String method : methods) {
				sb.append("    ").append(method).append("\n");
			}
		}
		return sb.toString();
	}
	
	public List<String> addMethods(Class aClass,boolean mandatory) {
		List<MemberInfo> memberInfos=ClassInfoUtil.getMemberInfo(aClass, false,true);
		List<String> methods = new ArrayList<String>();
		for (MemberInfo info : memberInfos ) {
			if (info.getMemberType().equals(MemberInfo.TYPE_METHOD)) {
				String methodImp = info.getFullDecleration().replace(";", "{}");
				boolean abstractMethod = info.getModifiers().indexOf("abstract") > -1;
				if (mandatory && abstractMethod ) {
					if (!mandatoryMethods.contains(methodImp)) {
						mandatoryMethods.add(methodImp);
						methods.add(methodImp);
					}
				} else if (!mandatory && !abstractMethod) {
					if (!optionalMethods.contains(methodImp)) {
						optionalMethods.add(methodImp);
						methods.add(methodImp);
					}
				}
			}
		}
		return methods;
	}
}

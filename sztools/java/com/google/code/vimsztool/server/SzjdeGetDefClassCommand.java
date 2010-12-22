package com.google.code.vimsztool.server;

import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_CLASSPATHXML;
import static com.google.code.vimsztool.server.SzjdeConstants.PARAM_EXP_TOKENS;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.code.vimsztool.omni.ClassInfo;


public class SzjdeGetDefClassCommand extends SzjdeCommand {

	public String execute() {
		String classPathXml = params.get(PARAM_CLASSPATHXML);
		String[] classNameList = params.get("classnames").split(",");
		String[] tokens = params.get(PARAM_EXP_TOKENS).split(",");
	    String sourceFile = params.get(SzjdeConstants.PARAM_SOURCEFILE);
		Class aClass = ClassInfo.getExistedClass(classPathXml, classNameList, sourceFile);
		if (aClass == null) return "";
		aClass = this.parseExpResultType(tokens, aClass);
		if (aClass == null) return "";
		return aClass.getName();
	}
	
	
	@SuppressWarnings("unchecked")
	private Class parseExpResultType(String[] tokens, Class aClass) {
		if (tokens == null || tokens.length == 0) return aClass;

		String memberType = "";
		for (int i=0; i<tokens.length; i++) {
			String token = tokens[i];
			if (token.trim().equals("") || token.equals(".") ) continue;
			if (token.indexOf("(") > 0) {
				memberType = "method";
				token=token.substring(0, token.indexOf("("));
			} else {
				memberType = "field";
			}
			if (i==tokens.length-1) {
				aClass = searchMemberTypeInHierarchy(aClass, token, memberType,true);
			} else {
				aClass = searchMemberTypeInHierarchy(aClass, token, memberType,false);
			}
			if (aClass == null) return null;
		}
		return aClass;
	}
	
	@SuppressWarnings("unchecked")
	private Class searchMemberTypeInHierarchy(Class aClass,String memberName,String memberType,boolean endToken) {
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
				if (Modifier.isPublic(mod) || ( Modifier.isProtected(mod)) ) {
					if (tf.getName().equals(memberName)) {
						if (!endToken) {
							if (memberType.equals("field")) {
								fClass = ((Field)tf).getType();
							} else {
								fClass = ((Method)tf).getReturnType();
							}
						} else {
							fClass = aClass;
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
	

}

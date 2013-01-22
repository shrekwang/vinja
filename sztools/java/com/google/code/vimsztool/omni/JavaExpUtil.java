package com.google.code.vimsztool.omni;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.code.vimsztool.util.ModifierFilter;

public class JavaExpUtil {
	
	@SuppressWarnings("unchecked")
	public static Class parseExpResultType(String[] tokens, Class aClass, ModifierFilter modifierFilter) {
		if (tokens == null || tokens.length == 0) return aClass;

		for (int i=0; i<tokens.length; i++) {
			String token = tokens[i].trim();
			if (token.equals("this") || token.equals("super")) continue;
			if (token.equals("") || token.equals(".") ) continue;
			String memberType = "";
			if (token.indexOf("(") > 0) {
				memberType = "method";
				token=token.substring(0, token.indexOf("("));
			} else {
				memberType = "field";
			}
			aClass = searchMemberInHierarchy(aClass, token,memberType ,modifierFilter,false);
			if (aClass == null) return null;
		}
		return aClass;
	}
	
	@SuppressWarnings("all")
	public static Class searchMemberInHierarchy(Class aClass, String memberName,
			String memberType, ModifierFilter modifierFilter, boolean classType) {
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
				if (tf.getName().equals(memberName)) {
					if (classType) {
						fClass = aClass;
					} else {
						if (memberType.equals("field")) {
							fClass = ((Field)tf).getType();
						} else {
							fClass = ((Method)tf).getReturnType();
						}
					}
					foundField = true;
					break;
				}
			}
			if (foundField) break;
			//try inner class
			if (memberType.equals("field")) {
				Class[] classes =aClass.getDeclaredClasses();
				for (Class clazz: classes) {
					if (clazz.getSimpleName().equals(memberName)) {
						foundField = true;
						fClass = clazz;
						break;
					}
				}
			}
			if (foundField) break;
			aClass = aClass.getSuperclass();
			if (aClass == null || aClass.getName().equals("java.lang.Object")) {
				break;
			}
		}
		return fClass;
	}
	
	@SuppressWarnings("unchecked")
	public static String getAllMember(Class aClass,String methodName) {
		
		List<MemberInfo> memberInfos=new ArrayList<MemberInfo>();
		LinkedList<Class> classList = ClassInfoUtil.getAllSuperClass(aClass);
	
		for (Class cls : classList) {
			List<MemberInfo> tmpInfoList = null;
			tmpInfoList=ClassInfoUtil.getMemberInfo(cls,false,true);
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
			sb.append(member.getReturnType()).append("\t");
			sb.append(member.getName()).append("(");
			sb.append(member.getParams()).append(")\t");
			sb.append(member.getExceptions()).append("\n");
		}
		return sb.toString();
	}

}

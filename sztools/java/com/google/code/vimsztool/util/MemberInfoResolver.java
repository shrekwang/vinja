package com.google.code.vimsztool.util;

import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CLASSMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_CONSTRUCTOR;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_INHERITMEMBER;
import static com.google.code.vimsztool.server.SzjdeConstants.CPT_TYPE_OBJECTMEMBER;

import java.util.ArrayList;
import java.util.List;

import com.google.code.vimsztool.compiler.CompilerContext;
import com.google.code.vimsztool.omni.ClassInfoUtil;
import com.google.code.vimsztool.omni.MemberInfo;
import com.google.code.vimsztool.parser.JavaSourceSearcher;

public class MemberInfoResolver {
	
	@SuppressWarnings("all")
	private static String resolveInSource(CompilerContext ctx, Class cls) {
		String className = cls.getName();
		
		//only resove java.xx.xx class with source
		if (! className.startsWith("java")) return null;
		String sourcePath = ctx.findSourceClass(cls.getName());
		if (sourcePath.equals("None")) return null;
		return sourcePath;
	}
	
	@SuppressWarnings("all")
	public static List<MemberInfo> resolveMemberInfo(CompilerContext ctx, Class cls) {
		
		List<MemberInfo> tmpInfoList = null;
		String sourcePath = resolveInSource(ctx, cls);
		
		if (sourcePath != null ) {
			JavaSourceSearcher searcher = JavaSourceSearcher.createSearcher(sourcePath, ctx);
			tmpInfoList = searcher.getMemberInfo(cls,false,true);
		} else {
			tmpInfoList=ClassInfoUtil.getMemberInfo(cls,false,true);
		}
		return tmpInfoList;
		
	}
	
	@SuppressWarnings("all")
	public static List<MemberInfo> resolveConstructorInfo(CompilerContext ctx,Class cls) {
		ArrayList<MemberInfo> memberInfos = null;
		String sourcePath = resolveInSource(ctx, cls);
		if (sourcePath != null) {
			JavaSourceSearcher searcher = JavaSourceSearcher.createSearcher(sourcePath, ctx);
			memberInfos = searcher.getConstructorInfo();
		} else {
			memberInfos = ClassInfoUtil.getConstructorInfo(cls);
		}
		return memberInfos;
	}
	
	@SuppressWarnings("all")
	public static List<MemberInfo> resolveMemberInfo(CompilerContext ctx, Class cls,boolean hasDotExp,String completionType) {
		List<MemberInfo> tmpInfoList = null;
	    String sourcePath = resolveInSource(ctx, cls);
		if (sourcePath != null ) {
			JavaSourceSearcher searcher = JavaSourceSearcher.createSearcher(sourcePath, ctx);
			tmpInfoList = getMemberList(searcher, completionType, hasDotExp, cls);
		} else {
			tmpInfoList = getMemberList(completionType, hasDotExp, cls);
		}
		
		return tmpInfoList;
	}
	
	@SuppressWarnings("all")
	private static List<MemberInfo> getMemberList(JavaSourceSearcher searcher , String completionType,boolean hasDotExp,Class cls) {
		List<MemberInfo> tmpInfoList = null;
		if (completionType.equals(CPT_TYPE_OBJECTMEMBER) || hasDotExp ) {
			tmpInfoList=searcher.getMemberInfo(cls,false,false);
		} else if (completionType.equals(CPT_TYPE_CLASSMEMBER)){
			tmpInfoList=searcher.getMemberInfo(cls,true,false);
		} else if (completionType.equals(CPT_TYPE_CONSTRUCTOR)){
			tmpInfoList=searcher.getConstructorInfo();
		} else if (completionType.equals(CPT_TYPE_INHERITMEMBER)){
			tmpInfoList=searcher.getMemberInfo(cls,false,true);
		}
		return tmpInfoList;
	}
	
	@SuppressWarnings("all")
	private static List<MemberInfo> getMemberList(String completionType,boolean hasDotExp,Class cls) {
		List<MemberInfo> tmpInfoList = null;
		if (completionType.equals(CPT_TYPE_OBJECTMEMBER) || hasDotExp ) {
			tmpInfoList=ClassInfoUtil.getMemberInfo(cls,false,false);
		} else if (completionType.equals(CPT_TYPE_CLASSMEMBER)){
			tmpInfoList=ClassInfoUtil.getMemberInfo(cls,true,false);
		} else if (completionType.equals(CPT_TYPE_CONSTRUCTOR)){
			tmpInfoList=ClassInfoUtil.getConstructorInfo(cls);
		} else if (completionType.equals(CPT_TYPE_INHERITMEMBER)){
			tmpInfoList=ClassInfoUtil.getMemberInfo(cls,false,true);
		}
		return tmpInfoList;
	}

}

package com.github.vinja.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.tree.CommonTree;

public interface IJavaSourceSearcher {

	List<MemberInfo> getMemberInfos();

	int getClassScopeLine();

	@SuppressWarnings("rawtypes")
	ArrayList<com.github.vinja.omni.MemberInfo> getMemberInfo(Class aClass, boolean staticMember, boolean protectedMember);

	ArrayList<com.github.vinja.omni.MemberInfo> getConstructorInfo();

	int searchLoopOutLine(int currentLine);

	Set<String> searchNearByExps(int lineNum, boolean currentLine);

	LocationInfo searchDefLocation(int line, int col, String sourceType);

}
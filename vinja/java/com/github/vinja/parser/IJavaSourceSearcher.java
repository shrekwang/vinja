package com.github.vinja.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface IJavaSourceSearcher {

	List<MemberInfo> getMemberInfos(String typeCanonicalName);

	@SuppressWarnings("rawtypes")
	ArrayList<com.github.vinja.omni.MemberInfo> getMemberInfo(Class aClass, boolean staticMember, boolean protectedMember);

	@SuppressWarnings("rawtypes")
	ArrayList<com.github.vinja.omni.MemberInfo> getConstructorInfo(Class aClass);

	int searchLoopOutLine(int currentLine);

	Set<String> searchNearByExps(int lineNum, boolean currentLine);

	LocationInfo searchDefLocation(int line, int col, String sourceType);

}
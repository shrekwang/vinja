package com.google.code.vimsztool.locate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtil {

	private static Map<String, List<Pattern>> patternCache = new HashMap<String, List<Pattern>>();

	private static List<Pattern> getPatternList(String excludes) {
		List<Pattern> tempPatterns = new ArrayList<Pattern>();
		if (excludes == null)
			return tempPatterns;
		if (patternCache.get(excludes) != null) {
			return patternCache.get(excludes);
		}

		String[] excludeStrs = excludes.split(",");
		for (String str : excludeStrs) {
			tempPatterns.add(translate(str));
		}
		patternCache.put(excludes, tempPatterns);
		return tempPatterns;
	}

	public static boolean isExclude(String excludes, File file) {
		List<Pattern> excludePatterns=getPatternList(excludes);
		if (excludePatterns.size() == 0) return false;
		String name = file.getAbsolutePath();
		for (Pattern pat : excludePatterns) {
			Matcher matcher = pat.matcher(name);
			if (matcher.matches())
				return true;
		}
		return false;
	}

	private static Pattern translate(String pat) {
		if (pat == null || pat.trim().equals(""))
			return null;
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < pat.length(); i++) {
			char c = pat.charAt(i);
			switch (c) {
			case '*':
				sb.append(".*");
				break;
			case '?':
				sb.append(".");
				break;
			case '/':
				sb.append(Pattern.quote(File.separator));
				break;
			default:
				sb.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(sb.toString());
	}
}

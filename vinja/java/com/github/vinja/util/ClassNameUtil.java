package com.github.vinja.util;

public class ClassNameUtil {

	public static String sourceToClassName(String sourcePath) {
		String locClassName = sourcePath.replace("/",".");
		if (locClassName.endsWith(".java")) {
			locClassName = locClassName.substring(0, locClassName.length()- 5);
		}
		return locClassName;
	}

}

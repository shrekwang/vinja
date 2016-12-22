package com.github.vinja.util;

import jd.commonide.IdeDecompiler;
import jd.commonide.preferences.IdePreferences;

public class DecompileUtil {

	public static String decompile(String jarPath, String innerPath) {
		boolean showDefaultConstructor = false;
		boolean realignmentLineNumber = true;
		boolean showPrefixThis = false;
		boolean mergeEmptyLines = false;
		boolean unicodeEscape = false;
		boolean showLineNumbers = false;
		boolean showMetadata = false;

		// Create preferences
		IdePreferences preferences = new IdePreferences(showDefaultConstructor, realignmentLineNumber, showPrefixThis,
				mergeEmptyLines, unicodeEscape, showLineNumbers, showMetadata);

		// Decompile
		String classContent = IdeDecompiler.decompile(preferences, jarPath, innerPath);
		return classContent;
	}
	
	public static void main(String[] args) {
		DecompileUtil.decompile("sdf", "sdf");
	}
}

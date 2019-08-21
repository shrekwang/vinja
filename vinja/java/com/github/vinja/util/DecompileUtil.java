package com.github.vinja.util;


public class DecompileUtil {

	public static String decompile(String jarPath, String innerPath) {
		boolean showDefaultConstructor = false;
		boolean realignmentLineNumber = true;
		boolean showPrefixThis = false;
		boolean mergeEmptyLines = false;
		boolean unicodeEscape = false;
		boolean showLineNumbers = false;
		boolean showMetadata = false;

		// Decompile
		//String classContent = IdeDecompiler.decompile(preferences, jarPath, innerPath);
		return "";
	}
	
	public static void main(String[] args) {
		DecompileUtil.decompile("sdf", "sdf");
	}
}

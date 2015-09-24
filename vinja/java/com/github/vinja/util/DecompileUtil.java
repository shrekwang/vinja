package com.github.vinja.util;

import jd.ide.intellij.JavaDecompiler;

import org.apache.commons.io.FilenameUtils;

public class DecompileUtil {

	public static String decompile(String jarPath, String innerPath) {
		String vinjaHome = Preference.getInstance().getVinjaHome();
		String jdcoreNativeLib = FilenameUtils.concat(vinjaHome, "lib/jdcore-native/win32/x86/jd-intellij.dll");
        if (OsCheck.isMac()) {
            jdcoreNativeLib = FilenameUtils.concat(vinjaHome, "lib/jdcore-native/macosx/x86_64/libjd-intellij.jnilib");
        }
		JavaDecompiler decompiler = JavaDecompiler.getInstance(jdcoreNativeLib);
		try {
			String t =  decompiler.decompile(jarPath, innerPath);
			return t;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
}

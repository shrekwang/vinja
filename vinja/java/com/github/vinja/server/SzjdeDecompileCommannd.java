package com.github.vinja.server;

import jd.ide.intellij.JavaDecompiler;

import org.apache.commons.io.FilenameUtils;

import com.github.vinja.util.Preference;


public class SzjdeDecompileCommannd extends SzjdeCommand {

	@SuppressWarnings("all")
	public String execute() {
		String jarPath = params.get("jarPath");
		String innerClass = params.get("innerPath");
		
		String vinjaHome = Preference.getInstance().getVinjaHome();
		String jdcoreNativeLib = FilenameUtils.concat(vinjaHome, "lib/jdcore-native/win32/x86/jd-intellij.dll");
		JavaDecompiler decompiler = JavaDecompiler.getInstance(jdcoreNativeLib);
		try {
			String t =  decompiler.decompile(jarPath, innerClass);
			return t;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

}
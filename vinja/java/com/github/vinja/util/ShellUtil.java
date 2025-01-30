package com.github.vinja.util;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class ShellUtil {
	
	public static void openTerminal(String path) {
		String ENV_OS = System.getProperty("os.name");
		String termCmd = "";
		if (ENV_OS.substring(0, 3).equalsIgnoreCase("win")) {
			termCmd = "cmd /c start";
		} else {
			termCmd = "gnome-terminal";
		}
		
		File termDir = new File(path);
		if (!termDir.isDirectory()) return;

		try {
			Runtime.getRuntime().exec(termCmd, null, termDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void openFileWithDefaultApp(String path) {

	}

}

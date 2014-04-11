package com.github.vinja.util;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.program.Program;

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
		if (path == null)
			return;
		String ext = FilenameUtils.getExtension(path);
		File file = new File(path);
		if (ext.equals("bat") || ext.equals("sh")) {
			try {
				Runtime.getRuntime().exec(new String[] { path }, null, file.getParentFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Program.launch(path);
		}

	}

}

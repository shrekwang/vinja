package com.google.code.vimsztool.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.FilenameUtils;

public class VjdeUtil {
	
	public static String getExceptionValue(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pwd = new PrintWriter(sw);
		e.printStackTrace(pwd);
		pwd.close();
		return sw.getBuffer().toString();
	}
	
	public static String getUserHome() {
		return System.getProperty("user.home");
	}

	public static String getToolDataHome() {
		File file = new File(FilenameUtils.concat(getUserHome(), ".sztools"	));
		if (!file.exists()) file.mkdir();
		return file.getPath();
	}

}

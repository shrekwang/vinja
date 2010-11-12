package com.google.code.vimsztool.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class VjdeUtil {
	
	public static String getExceptionValue(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pwd = new PrintWriter(sw);
		e.printStackTrace(pwd);
		pwd.close();
		return sw.getBuffer().toString();
	}

}

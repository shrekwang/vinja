package com.google.code.vimsztool.util;

import java.io.File;
import java.io.IOException;
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
	

	public static String getToolDataHome() {
		String userHome = System.getProperty("user.home");
		File file = new File(FilenameUtils.concat(userHome, ".sztools"	));
		if (!file.exists()) file.mkdir();
		return file.getPath();
	}
	
	public static void runVimCmd(String serverName, String[] cmdLine) {
		try {
			StringBuffer sb = new StringBuffer();
			for (String arg : cmdLine ) {
				arg = arg.replace("'", "''");
				sb.append(arg).append(" ");
			}
			String vimCmdCall="<esc><esc>:"+sb.toString()+"<cr>";
			String[] vimRemoteCmdArray = new String[] {"gvim","--servername",serverName,"--remote-send",	vimCmdCall};
			Runtime.getRuntime().exec(vimRemoteCmdArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

package com.github.vinja.util;

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
		File file = new File(FilenameUtils.concat(userHome, ".vinja"	));
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
			//String[] vimRemoteCmdArray = new String[] {"C:\\Program Files\\Vim\\vim73\\gvim.exe","--servername",serverName,"--remote-send",	vimCmdCall};
			Runtime.getRuntime().exec(vimRemoteCmdArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean simpleMatch(String value, String pat) {
        int patLen = pat.length();
        int valueLen = value.length();
        if (valueLen < patLen ) return false;
        if (value.charAt(0) != pat.charAt(0)) return false;

        int valueIndex = 0;
        boolean noMatch = false;
        for (int i=0; i<patLen; i++) {
            if ( value.charAt(valueIndex) == pat.charAt(i)) {
                valueIndex++;
            } else {
                while (value.charAt(valueIndex) != pat.charAt(i)) {
                    if ( valueIndex >= valueLen - 1 ) {
                        noMatch = true;
                        break;
                    }
                    valueIndex++;
                }
            }
            if (noMatch) return false;
        }
        return true;
    }
	
	public static synchronized void callVimFunc(String serverName, String funcName, String[] args) {
		
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(funcName).append("(");
			// '"' and '\' needs to be escaped in command line.
			if (args !=null && args.length > 0) {
				for (int i=0; i<args.length; i++ ) {
					String arg = args[i];
					arg = arg.replace("'", "''");
					//arg = arg.replace("\\", "\\\\");
					sb.append("'").append(arg).append("'");
					if (i<args.length-1) {
						sb.append(",");
					}
				}
			}
			sb.append(")");
			String[] vimRemoteCmdArray = new String[] {"vim","--servername",serverName,"--remote-expr",sb.toString()};
			Runtime.getRuntime().exec(vimRemoteCmdArray);
			//prevent call vim command in multi thread simultaneously
			Thread.sleep(20);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}

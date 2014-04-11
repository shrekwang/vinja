package com.github.vinja.util;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

public class JdeLogger {

	private static SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
	
	private String logName ;
	private String logPath ;
	
	public static JdeLogger getLogger(String name) {
		return new JdeLogger(name);
	}
	
	private JdeLogger(String name) {
		logPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "JdeServer.log");
		this.logName = name;
	}
	
	public void info(String msg) {
		writeMsg("INFO",msg);
	}
	
	public void warn(String msg) {
		writeMsg("WARN",msg);
	}
	
	private void writeMsg(String level, String msg) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(logPath,true));
			pw.println(buildMsg(level,msg));
			pw.close();
		} catch (Throwable e) {
			if (pw !=null) pw.close();
		}
	}

	private String buildMsg(String level,String msg) {
		return simpleFormat.format(new Date())+ ":[" + this.logName + "]" + level + ":" + msg  ;
	}

}


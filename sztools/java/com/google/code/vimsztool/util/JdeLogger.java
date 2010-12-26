package com.google.code.vimsztool.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

public class JdeLogger {

	private static FileHandler fileHandler;

	public static Logger getLogger(String name) {
		Logger log = Logger.getLogger(name);
		log.setLevel(Level.INFO);
		initHandler();
		if (fileHandler != null) {
			log.addHandler(fileHandler);
		}
		return log;
	}

	public static void initHandler() {
		try {
			if (fileHandler != null)
				return;
			String logPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "JdeServer.log");
			fileHandler = new FileHandler(logPath);
			fileHandler.setLevel(Level.INFO);
			fileHandler.setFormatter(new JdeLogHander());
		} catch (IOException e) {
		}
	}

}

class JdeLogHander extends Formatter {
	public String format(LogRecord record) {
		return "[" + record.getClass() + "]" + record.getLevel() + ":" + record.getMessage() + "\n";
	}
}

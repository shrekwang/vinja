package com.google.code.vimsztool.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.util.VjdeUtil;

public class TomcatJvmoptConf {
	
	private static StringBuffer jvmOptsBuffer = null;

	private static void loadVarsFromFile(File file) {
		
		jvmOptsBuffer = new StringBuffer("");
		if (file == null)  {
			return ;
		}
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			while (true) {
				String tmp = br.readLine();
				if (tmp == null) break;
				if (tmp.startsWith("#")) continue;
				jvmOptsBuffer.append(tmp).append(" ");
			}
		} catch (IOException e) {
			
		} finally {
			if (br != null) try { br.close(); } catch (Exception e) {}
		}
			
	}
	
	public static String getJvmOptions() {
		if (jvmOptsBuffer == null) {
			loadVarsFromFile(getConfigFile());
		}
		return jvmOptsBuffer.toString();
	}

	private static File getConfigFile() {
		String userCfgPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "tomcat_jvmopt.cfg");
		File tmpFile = new File(userCfgPath);
		if (tmpFile.exists()) return tmpFile;
		return null;
	}

}

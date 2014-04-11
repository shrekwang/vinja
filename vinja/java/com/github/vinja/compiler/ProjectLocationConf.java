package com.github.vinja.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.github.vinja.util.VjdeUtil;

public class ProjectLocationConf {
	
	private static Map<String,String> config  = null;

	private static void loadVarsFromFile(File file) {
		config = new HashMap<String,String>();
		
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader(file));
			while (true) {
				String tmp = br.readLine();
				if (tmp == null) break;
				if (tmp.startsWith("#")) continue;
				int splitIndex = tmp.indexOf("=");
				if (splitIndex < 0 ) continue;
				String name = tmp.substring(0,splitIndex).trim();
				String path = tmp.substring(splitIndex+1).trim();
				config.put(name, path);
			}
		} catch (IOException e) {
			
		} finally {
			if (br != null) try { br.close(); } catch (Exception e) {}
		}
			
	}
	
	public static String getProjectLocation(String name) {
		if (config == null) {
			loadVarsFromFile(getConfigFile());
		}
		String value= config.get(name);
		return value ;
	}

	private static File getConfigFile() {
		String userCfgPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "project.cfg");
		File tmpFile = new File(userCfgPath);
		if (tmpFile.exists()) return tmpFile;
		return null;
	}

}

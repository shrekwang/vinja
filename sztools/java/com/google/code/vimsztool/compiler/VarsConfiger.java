package com.google.code.vimsztool.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;

import com.google.code.vimsztool.util.Preference;
import com.google.code.vimsztool.util.VjdeUtil;

public class VarsConfiger {
	
	private static Map<String,String> config  = null;

	private static void loadVarsFromFile(File file) {
		config = new HashMap<String,String>();
		if (!file.exists() || !file.canRead()) return ;
		Properties prop  = new Properties();
		try {
			prop.load(new FileInputStream(file));
			for (Object key : prop.keySet() ) {
				String keyStr = (String)key;
				String value = prop.getProperty(keyStr, "");
				config.put(keyStr, value);
			}
		} catch (Exception e) { 
		}
	}
	
	public static String getVarValue(String name) {
		if (config == null) {
			loadVarsFromFile(getConfigFile());
		}
		String value= config.get(name);
		return value == null ? "" : value;
	}

	private static File getConfigFile() {
		String sztoolHome = Preference.getInstance().getSztoolHome();
		String userCfgPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "vars.txt");
		File tmpFile = new File(userCfgPath);
		if (tmpFile.exists()) return tmpFile;
		
		String defaultCfgPath = FilenameUtils.concat(sztoolHome, "share/conf/vars.txt");
		tmpFile = new File(defaultCfgPath);
		if (tmpFile.exists()) return tmpFile;
		
		return null;
	}

}

package com.github.vinja.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;


public class Preference {
	
	private static Preference instance  = new Preference();
	private Map<String,String> config  = new HashMap<String,String>();
	private String vinjaHome = null;
	
	private Preference() {}
	
	public static final String JDE_COMPILE_ENCODING = "jde_compile_encoding" ; 
	public static final String JDE_COMPILE_IGNORE_WARING = "jde_compile_ignore_waring" ; 
	public static final String JDE_SRC_VM = "jde_src_vm" ; 
	public static final String JDE_DST_VM = "jde_dst_vm" ; 
	public static final String JDE_SERVER_PORT = "jde_server_port" ; 
	public static final String JDE_ECLIPSE_CONXML_PATH = "jde_eclipse_conxml_path" ; 
	public static final String DEFAULT_EXCLUDE_PATTERN = "default_exclude_pattern";
	public static final String JDE_RUN_TIMEOUT = "jde_run_timeout";
	
	public static final String TOMCAT_HOME = "tomcat_home";
	public static final String TOMCAT_VERSION = "tomcat_version";

	
	public static Preference getInstance() {
		return instance;
	}
	
	public void init(String vinjaHome) {
		this.vinjaHome = vinjaHome;
		String defaultCfgPath = FilenameUtils.concat(vinjaHome, "share/conf/vinja.cfg");
		loadPrefFromFile(defaultCfgPath);
		String userCfgPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "vinja.cfg");
		loadPrefFromFile(userCfgPath);
	}
	
	private void loadPrefFromFile(String filePath) {
		File file = new File(filePath);
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
	
	public String getVinjaHome() {
		return vinjaHome;
	}
	
	public String getValue(String key) {
		String value= config.get(key);
		return value == null ? "" : value;
	}
	
}
